/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */


/**
 *
 * @author arindam
 */
package com.mealapp.dao;

import com.mealapp.model.Given;
import com.mealapp.util.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GivenDAO {

    // insert given (student gives money to manager)
    public void insert(Given g) throws Exception {
        String sql = "INSERT INTO given (student_id, amount, given_date) VALUES (?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, g.getStudentId());
            ps.setBigDecimal(2, g.getAmount());
            ps.setDate(3, Date.valueOf(g.getGivenDate()));
            ps.executeUpdate();
        }
    }

    // delete given entry
    public void delete(int id) throws Exception {
        String sql = "DELETE FROM given WHERE id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // list given entries between dates
    public List<Given> listBetween(LocalDate from, LocalDate to) throws Exception {
        List<Given> list = new ArrayList<>();
        String sql =
        "SELECT id, student_id, amount, given_date " +
        "FROM given " +
        "WHERE given_date BETWEEN ? AND ? " +
        "ORDER BY given_date";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Given g = new Given();
                    g.setId(rs.getInt("id"));
                    g.setStudentId(rs.getInt("student_id"));
                    g.setAmount(rs.getBigDecimal("amount"));
                    g.setGivenDate(rs.getDate("given_date").toLocalDate());
                    list.add(g);
                }
            }
        }
        return list;
    }

    // total money given in a date range
    public BigDecimal sumBetween(LocalDate from, LocalDate to) throws Exception {
        String sql = "SELECT COALESCE(SUM(amount),0) FROM given WHERE given_date BETWEEN ? AND ?";
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

    public Map<Integer, BigDecimal> sumByStudentBetween(LocalDate from, LocalDate to) throws Exception {
    Map<Integer, BigDecimal> map = new HashMap<>();
    String sql = "SELECT student_id, SUM(amount) AS total "
           + "FROM given "
           + "WHERE given_date BETWEEN ? AND ? "
           + "GROUP BY student_id";

    try (Connection c = DBConnection.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {

        ps.setDate(1, java.sql.Date.valueOf(from));
        ps.setDate(2, java.sql.Date.valueOf(to));

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(
                    rs.getInt("student_id"),
                    rs.getBigDecimal("total")
                );
            }
        }
    }

    return map;
}

    public List<Integer> studentsNotContributed(LocalDate from, LocalDate to) throws Exception {
    List<Integer> list = new ArrayList<>();
    String sql = "SELECT s.id " +
             "FROM students s " +
             "LEFT JOIN given g ON s.id = g.student_id AND g.given_date BETWEEN ? AND ? " +
             "WHERE g.id IS NULL";

    try (Connection c = DBConnection.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setDate(1, Date.valueOf(from));
        ps.setDate(2, Date.valueOf(to));
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getInt("id"));
            }
        }
    }

    return list;
}



}