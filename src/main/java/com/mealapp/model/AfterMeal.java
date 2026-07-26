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

public class AfterMeal {
    private int id;
    private int studentId;
    private int numberOfMeal;
    private LocalDate mealDate;
    // getters/setters...
     public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public int getNumberOfMeal() {
        return numberOfMeal;
    }

    public void setNumberOfMeal(int numberOfMeal) {
        this.numberOfMeal = numberOfMeal;
    }

    public LocalDate getMealDate() {
        return mealDate;
    }

    public void setMealDate(LocalDate mealDate) {
        this.mealDate = mealDate;
    }
}