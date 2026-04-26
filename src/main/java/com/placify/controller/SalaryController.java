package com.placify.controller;

import com.placify.model.SalaryBreakdown;
import com.placify.model.User;
import com.placify.repository.SalaryBreakdownRepository;
import com.placify.repository.UserRepository;
import com.placify.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/salary")
@CrossOrigin(origins = "http://localhost:5173")
public class SalaryController {

    @Autowired private SalaryBreakdownRepository salaryBreakdownRepository;
    @Autowired private UserRepository userRepository;

    @PostMapping("/decode")
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE', 'ADMIN')")
    public ResponseEntity<?> decodeSalary(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        double ctc = ((Number) request.get("totalCtc")).doubleValue();
        String city = (String) request.getOrDefault("city", "metro");
        String regime = (String) request.getOrDefault("taxRegime", "new");
        boolean pfOptOut = Boolean.TRUE.equals(request.get("pfOptOut"));

        Map<String, Object> result = calculateSalary(ctc, city, regime, pfOptOut);

        // Save to DB
        SalaryBreakdown sb = new SalaryBreakdown();
        sb.setTotalCtc(ctc);
        sb.setBaseSalary((Double) result.get("basic"));
        sb.setHra((Double) result.get("hra"));
        sb.setBonus((Double) result.get("variablePay"));
        sb.setPf((Double) result.get("pfEmployee"));
        sb.setInsurance((Double) result.get("insurance"));
        sb.setOtherBenefits((Double) result.get("otherAllowances"));
        sb.setInHandSalary((Double) result.get("monthlyInHand"));
        sb.setUser(user);
        salaryBreakdownRepository.save(sb);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/my-breakdowns")
    @PreAuthorize("hasAnyRole('USER', 'EMPLOYEE', 'ADMIN')")
    public List<SalaryBreakdown> getMyBreakdowns(@AuthenticationPrincipal UserPrincipal currentUser) {
        return salaryBreakdownRepository.findByUserId(currentUser.getId());
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<SalaryBreakdown> getAllBreakdowns() {
        return salaryBreakdownRepository.findAll();
    }

    // ── Core salary calculation logic ─────────────────────────────────────────

    private Map<String, Object> calculateSalary(double ctc, String city, String regime, boolean pfOptOut) {
        Map<String, Object> r = new HashMap<>();

        // ── 1. CTC Components ─────────────────────────────────────────────────
        double basic        = round(ctc * 0.40);          // 40% of CTC
        double hra          = round(basic * 0.50);         // 50% of basic (metro) or 40%
        double variablePay  = round(ctc * 0.10);           // 10% variable/bonus
        double insurance    = round(Math.min(ctc * 0.005, 15000)); // group health insurance
        double gratuity     = round(basic * 0.0481);       // 4.81% of basic (employer)

        // PF: 12% of basic, capped at ₹1800/month (₹21600/year) if basic > 15000
        double pfEmployee   = pfOptOut ? 0 : round(Math.min(basic * 0.12, 21600));
        double pfEmployer   = round(Math.min(basic * 0.12, 21600));  // employer contribution (part of CTC)

        // Other allowances = CTC - all above components
        double otherAllowances = round(ctc - basic - hra - variablePay - insurance - gratuity - pfEmployer);
        otherAllowances = Math.max(otherAllowances, 0);

        // ── 2. Gross Salary (monthly) ─────────────────────────────────────────
        double grossMonthly = round((basic + hra + otherAllowances + variablePay / 12) / 12);

        // ── 3. HRA Exemption ─────────────────────────────────────────────────
        // Least of: actual HRA | 50% basic (metro) / 40% basic (non-metro) | rent paid - 10% basic
        double hraMonthly   = round(hra / 12);
        double basicMonthly = round(basic / 12);
        double hraExemption;
        boolean isMetro = city.equalsIgnoreCase("metro");
        // Assume rent = 30% of basic for calculation (user can adjust)
        double assumedRent  = round(basicMonthly * 0.30);
        double hraLimit1    = hraMonthly;
        double hraLimit2    = isMetro ? round(basicMonthly * 0.50) : round(basicMonthly * 0.40);
        double hraLimit3    = Math.max(0, assumedRent - round(basicMonthly * 0.10));
        hraExemption        = round(Math.min(Math.min(hraLimit1, hraLimit2), hraLimit3));

        // ── 4. Standard Deduction & other deductions ─────────────────────────
        double standardDeduction = 50000; // flat ₹50,000 per year
        double pfDeduction       = pfEmployee; // 80C
        double npsDeduction      = 0;          // optional, not included

        // ── 5. Taxable Income ─────────────────────────────────────────────────
        double grossAnnual       = basic + hra + otherAllowances + variablePay;
        double hraExemptionAnnual = hraExemption * 12;

        double taxableIncome;
        double incomeTax;

        if (regime.equalsIgnoreCase("old")) {
            // Old regime: HRA exemption + standard deduction + 80C (PF)
            double totalDeductions = hraExemptionAnnual + standardDeduction + Math.min(pfDeduction, 150000);
            taxableIncome = Math.max(0, grossAnnual - totalDeductions);
            incomeTax = calculateOldRegimeTax(taxableIncome);
        } else {
            // New regime (FY 2024-25): standard deduction ₹75,000, no other exemptions
            taxableIncome = Math.max(0, grossAnnual - 75000);
            incomeTax = calculateNewRegimeTax(taxableIncome);
        }

        // ── 6. Surcharge & Cess ───────────────────────────────────────────────
        double surcharge = 0;
        if (taxableIncome > 5000000 && taxableIncome <= 10000000) surcharge = incomeTax * 0.10;
        else if (taxableIncome > 10000000) surcharge = incomeTax * 0.15;
        double cess = round((incomeTax + surcharge) * 0.04); // 4% health & education cess
        double totalTax = round(incomeTax + surcharge + cess);

        // ── 7. Monthly deductions ─────────────────────────────────────────────
        double monthlyTax       = round(totalTax / 12);
        double monthlyPf        = round(pfEmployee / 12);
        double monthlyInsurance = round(insurance / 12);
        double professionalTax  = getProfessionalTax(grossMonthly); // state-based

        // ── 8. Monthly In-Hand ────────────────────────────────────────────────
        double monthlyInHand = round(grossMonthly - monthlyTax - monthlyPf - monthlyInsurance - professionalTax);

        // ── 9. Effective tax rate ─────────────────────────────────────────────
        double effectiveTaxRate = grossAnnual > 0 ? round((totalTax / grossAnnual) * 100) : 0;

        // ── Build response ────────────────────────────────────────────────────
        r.put("ctc",              round(ctc));
        r.put("basic",            round(basic));
        r.put("hra",              round(hra));
        r.put("variablePay",      round(variablePay));
        r.put("insurance",        round(insurance));
        r.put("gratuity",         round(gratuity));
        r.put("pfEmployee",       round(pfEmployee));
        r.put("pfEmployer",       round(pfEmployer));
        r.put("otherAllowances",  round(otherAllowances));
        r.put("grossAnnual",      round(grossAnnual));
        r.put("grossMonthly",     grossMonthly);
        r.put("taxableIncome",    round(taxableIncome));
        r.put("incomeTax",        round(incomeTax));
        r.put("surcharge",        round(surcharge));
        r.put("cess",             cess);
        r.put("totalTax",         totalTax);
        r.put("monthlyTax",       monthlyTax);
        r.put("monthlyPf",        monthlyPf);
        r.put("monthlyInsurance", monthlyInsurance);
        r.put("professionalTax",  professionalTax);
        r.put("monthlyInHand",    monthlyInHand);
        r.put("annualInHand",     round(monthlyInHand * 12));
        r.put("effectiveTaxRate", effectiveTaxRate);
        r.put("hraExemption",     round(hraExemptionAnnual));
        r.put("taxRegime",        regime);
        r.put("city",             city);

        // Percentages for chart
        r.put("pctBasic",         pct(basic, ctc));
        r.put("pctHra",           pct(hra, ctc));
        r.put("pctVariable",      pct(variablePay, ctc));
        r.put("pctPf",            pct(pfEmployee + pfEmployer, ctc));
        r.put("pctTax",           pct(totalTax, ctc));
        r.put("pctInsurance",     pct(insurance, ctc));
        r.put("pctOther",         pct(otherAllowances, ctc));

        return r;
    }

    // New Tax Regime FY 2024-25 slabs
    private double calculateNewRegimeTax(double income) {
        if (income <= 300000)  return 0;
        if (income <= 700000)  return (income - 300000) * 0.05;
        if (income <= 1000000) return 20000 + (income - 700000) * 0.10;
        if (income <= 1200000) return 50000 + (income - 1000000) * 0.15;
        if (income <= 1500000) return 80000 + (income - 1200000) * 0.20;
        return 140000 + (income - 1500000) * 0.30;
    }

    // Old Tax Regime slabs
    private double calculateOldRegimeTax(double income) {
        if (income <= 250000)  return 0;
        if (income <= 500000)  return (income - 250000) * 0.05;
        if (income <= 1000000) return 12500 + (income - 500000) * 0.20;
        return 112500 + (income - 1000000) * 0.30;
    }

    // Professional tax (Maharashtra rates as default)
    private double getProfessionalTax(double monthlyGross) {
        if (monthlyGross <= 7500)  return 0;
        if (monthlyGross <= 10000) return 175;
        return 200;
    }

    private double round(double v) { return Math.round(v * 100.0) / 100.0; }
    private double pct(double part, double total) { return total > 0 ? round((part / total) * 100) : 0; }
}
