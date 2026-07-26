/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */


/**
 *
 * @author arindam
 */
package com.mealapp.dao;

import com.mealapp.model.Expense;
import com.mealapp.util.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ExpenseDAO {

    // insert expense (manager spends money)
    public void insert(Expense e) throws Exception {
        String sql = "INSERT INTO expenses (student_id, amount, expense_date, description) " +
             "VALUES (?, ?, ?, ?)";
        
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (e.getStudentId() == null)
                ps.setNull(1, Types.INTEGER);
            else
                ps.setInt(1, (int) e.getStudentId());

            ps.setBigDecimal(2, (BigDecimal) e.getAmount());
            ps.setDate(3, Date.valueOf(e.getExpenseDate()));
            ps.setString(4, (String) e.getDescription());
            ps.executeUpdate();
        }
    }

    // delete expense
    public void delete(int id) throws Exception {
        String sql = "DELETE FROM expenses WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // list expenses between dates
    public List<Expense> listBetween(LocalDate from, LocalDate to) throws Exception {
        List<Expense> list = new ArrayList<>();
        String sql =
        "SELECT id, student_id, amount, expense_date, description " +
        "FROM expenses " +
        "WHERE expense_date BETWEEN ? AND ? " +
        "ORDER BY expense_date";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Expense e = new Expense();
                    e.setId(rs.getInt("id"));
                    int sid = rs.getInt("student_id");
                    e.setStudentId(rs.wasNull() ? null : sid);
                    e.setAmount(rs.getBigDecimal("amount"));
                    e.setExpenseDate(rs.getDate("expense_date").toLocalDate());
                    e.setDescription(rs.getString("description"));
                    list.add(e);
                }
            }
        }
        return list;
    }

    // total expenses between dates (used for meal rate)
    public BigDecimal sumBetween(LocalDate from, LocalDate to) throws Exception {
        String sql = "SELECT COALESCE(SUM(amount),0) " +
                     "FROM expenses " +
                     "WHERE expense_date BETWEEN ? AND ?";


        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal(1);
            }
        }
        return BigDecimal.ZERO;
    }

public Iterable<Expense> findAll() {
    List<Expense> list = new ArrayList<>();

    String sql = "SELECT id, student_id, amount, expense_date, description " +
             "FROM expenses " +
             "ORDER BY expense_date";


    try (Connection c = DBConnection.getConnection();
         PreparedStatement ps = c.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

        while (rs.next()) {
            Expense e = new Expense();
            e.setId(rs.getInt("id"));

            int sid = rs.getInt("student_id");
            e.setStudentId(rs.wasNull() ? null : sid);

            e.setAmount(rs.getBigDecimal("amount"));
            e.setExpenseDate(rs.getDate("expense_date").toLocalDate());
            e.setDescription(rs.getString("description"));

            list.add(e);
        }

    } catch (Exception ex) {
        ex.printStackTrace();
    }

    return list;
}


public void create(Expense e) {
    String sql = "INSERT INTO expenses (student_id, amount, expense_date, description) " +
             "VALUES (?, ?, ?, ?)";

    try (Connection c = DBConnection.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {

        if (e.getStudentId() == null) {
            ps.setNull(1, Types.INTEGER);
        } else {
            ps.setInt(1, e.getStudentId());
        }

        ps.setBigDecimal(2, e.getAmount());
        ps.setDate(3, Date.valueOf(e.getExpenseDate()));
        ps.setString(4, e.getDescription());

        ps.executeUpdate();

    } catch (Exception ex) {
        ex.printStackTrace();
    }
}

}
