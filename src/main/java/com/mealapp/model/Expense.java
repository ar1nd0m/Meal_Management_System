/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author arindam
 */
package com.mealapp.model;

import java.time.LocalDate;
import java.math.BigDecimal;

public class Expense {

    private int id;
    private Integer studentId;   // nullable (manager expense)
    private BigDecimal amount;
    private LocalDate expenseDate;
    private String description;

    // ---- Constructors ----
    public Expense() {
    }

    public Expense(int id, Integer studentId, BigDecimal amount,
                   LocalDate expenseDate, String description) {
        this.id = id;
        this.studentId = studentId;
        this.amount = amount;
        this.expenseDate = expenseDate;
        this.description = description;
    }

    // ---- Getters ----
    public int getId() {
        return id;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public String getDescription() {
        return description;
    }

    // ---- Setters ----
    public void setId(int id) {
        this.id = id;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

