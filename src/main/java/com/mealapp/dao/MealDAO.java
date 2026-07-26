/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author arindam
 */
package com.mealapp.dao;

import com.mealapp.model.BeforeMeal;
import com.mealapp.model.AfterMeal;
import com.mealapp.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MealDAO {
    // Save before meal
    public void saveBefore(int studentId, int numberOfMeals, LocalDate date) throws Exception {
        String sql = "INSERT INTO before_meal_call (student_id, number_of_meal, meal_date) VALUES (?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, numberOfMeals);
            ps.setDate(3, java.sql.Date.valueOf(date));
            ps.executeUpdate();
        }
    }

    // Save after meal
    public void saveAfter(int studentId, int numberOfMeals, LocalDate date) throws Exception {
        String sql = "INSERT INTO after_meal_call (student_id, number_of_meal, meal_date) VALUES (?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, numberOfMeals);
            ps.setDate(3, java.sql.Date.valueOf(date));
            ps.executeUpdate();
        }
    }

    // Get counts for a month-range for before or after
    public List<com.mealapp.model.AfterMeal> listAfterBetween(LocalDate from, LocalDate to) throws Exception {
    String sql = "SELECT id, student_id, number_of_meal, meal_date " +
                 "FROM after_meal_call WHERE meal_date BETWEEN ? AND ?";
    List<com.mealapp.model.AfterMeal> list = new ArrayList<>();

    try (Connection c = DBConnection.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {

        ps.setDate(1, Date.valueOf(from));
        ps.setDate(2, Date.valueOf(to));

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                com.mealapp.model.AfterMeal m = new com.mealapp.model.AfterMeal();
                m.setId(rs.getInt("id"));
                m.setStudentId(rs.getInt("student_id"));
                m.setNumberOfMeal(rs.getInt("number_of_meal"));
                m.setMealDate(rs.getDate("meal_date").toLocalDate());
                list.add(m);
            }
        }
    }
    return list;
}

    public List<BeforeMeal> listBeforeAll() throws Exception {
    List<BeforeMeal> list = new ArrayList<>();
    String sql = "SELECT * FROM before_meal_call ORDER BY meal_date";
    try (Connection c = DBConnection.getConnection();
         PreparedStatement ps = c.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            BeforeMeal m = new BeforeMeal();
            m.setId(rs.getInt("id"));
            m.setStudentId(rs.getInt("student_id"));
            m.setNumberOfMeal(rs.getInt("number_of_meal"));
            m.setMealDate(rs.getDate("meal_date").toLocalDate());
            list.add(m);
        }
    }
    return list;
}

public List<AfterMeal> listAfterAll() throws Exception {
    List<AfterMeal> list = new ArrayList<>();
    String sql = "SELECT * FROM after_meal_call ORDER BY meal_date";
    try (Connection c = DBConnection.getConnection();
         PreparedStatement ps = c.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            AfterMeal m = new AfterMeal();
            m.setId(rs.getInt("id"));
            m.setStudentId(rs.getInt("student_id"));
            m.setNumberOfMeal(rs.getInt("number_of_meal"));
            m.setMealDate(rs.getDate("meal_date").toLocalDate());
            list.add(m);
        }
    }
    return list;
}

public void deleteBefore(int id) throws Exception {
    String sql = "DELETE FROM before_meal_call WHERE id=?";
    try (Connection c = DBConnection.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setInt(1, id);
        ps.executeUpdate();
    }
}

public void deleteAfter(int id) throws Exception {
    String sql = "DELETE FROM after_meal_call WHERE id=?";
    try (Connection c = DBConnection.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setInt(1, id);
        ps.executeUpdate();
    }
}

    // similar listAfterBetween...

    // in com.mealapp.dao.MealDAO
public List<BeforeMeal> listBeforeBetween(LocalDate from, LocalDate to) throws Exception {
    String sql = "SELECT id, student_id, number_of_meal, meal_date " +
                 "FROM before_meal_call WHERE meal_date BETWEEN ? AND ? ORDER BY meal_date";
    List<BeforeMeal> list = new ArrayList<>();
    try (Connection c = DBConnection.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setDate(1, Date.valueOf(from));
        ps.setDate(2, Date.valueOf(to));
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                BeforeMeal m = new BeforeMeal();
                m.setId(rs.getInt("id"));
                m.setStudentId(rs.getInt("student_id"));
                m.setNumberOfMeal(rs.getInt("number_of_meal"));
                m.setMealDate(rs.getDate("meal_date").toLocalDate());
                list.add(m);
            }
        }
    }
    return list;
}

}
