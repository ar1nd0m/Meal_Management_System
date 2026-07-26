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

public class Given {

    private int id;
    private int studentId;
    private BigDecimal amount;
    private LocalDate givenDate;

    // no-arg constructor
    public Given() {
    }

    // optional full constructor
    public Given(int id, int studentId, BigDecimal amount, LocalDate givenDate) {
        this.id = id;
        this.studentId = studentId;
        this.amount = amount;
        this.givenDate = givenDate;
    }

    // getters
    public int getId() {
        return id;
    }

    public int getStudentId() {
        return studentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getGivenDate() {
        return givenDate;
    }

    // setters
    public void setId(int id) {
        this.id = id;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setGivenDate(LocalDate givenDate) {
        this.givenDate = givenDate;
    }
}
