package com.placify.model;

import javax.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "salary_breakdowns")
public class SalaryBreakdown {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Double totalCtc;
    private Double baseSalary;
    private Double hra;
    private Double bonus;
    private Double pf;
    private Double insurance;
    private Double otherBenefits;
    private Double inHandSalary;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
