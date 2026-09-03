package inventorymanagementsystem.dao;

import inventorymanagementsystem.db.DBConnection;
import inventorymanagementsystem.model.Product;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access class for the `product` table. Every method opens its
 * connection via {@link DBConnection}, and closes its own
 * Statement/ResultSet through try-with-resources.
 */
public class ProductDAO {

    /** One page of products, optionally filtered by name/category/brand. */
    public List<Product> getProducts(int pageIndex, int pageSize, String searchTerm) {
        List<Product> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT idno, name, description, category, brand, price, stock FROM product");
        boolean hasSearch = searchTerm != null && !searchTerm.trim().isEmpty();
        if (hasSearch) {
            sql.append(" WHERE name LIKE ? OR category LIKE ? OR brand LIKE ?");
        }
        sql.append(" ORDER BY idno LIMIT ? OFFSET ?");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int i = 1;
            if (hasSearch) {
                String like = "%" + searchTerm.trim() + "%";
                ps.setString(i++, like);
                ps.setString(i++, like);
                ps.setString(i++, like);
            }
            ps.setInt(i++, pageSize);
            ps.setInt(i, Math.max(0, (pageIndex - 1) * pageSize));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int getTotalCount(String searchTerm) {
        String sql = "SELECT COUNT(*) FROM product";
        boolean hasSearch = searchTerm != null && !searchTerm.trim().isEmpty();
        if (hasSearch) {
            sql += " WHERE name LIKE ? OR category LIKE ? OR brand LIKE ?";
        }
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (hasSearch) {
                String like = "%" + searchTerm.trim() + "%";
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public Product getById(int idno) {
        String sql = "SELECT idno, name, description, category, brand, price, stock FROM product WHERE idno = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idno);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean insert(Product p) {
        String sql = "INSERT INTO product (name, description, category, brand, price, stock) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getDescription());
            ps.setString(3, p.getCategory());
            ps.setString(4, p.getBrand());
            ps.setBigDecimal(5, p.getPrice());
            ps.setInt(6, p.getStock());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        p.setIdno(keys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean update(Product p) {
        String sql = "UPDATE product SET name=?, description=?, category=?, brand=?, price=?, stock=? WHERE idno=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getDescription());
            ps.setString(3, p.getCategory());
            ps.setString(4, p.getBrand());
            ps.setBigDecimal(5, p.getPrice());
            ps.setInt(6, p.getStock());
            ps.setInt(7, p.getIdno());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

// added .
public boolean delete(int idno) {
    String deleteInventorySql = "DELETE FROM inventory WHERE product_id = ?";

    String deleteProductSql = "DELETE FROM product WHERE idno = ?";

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement psInventory = conn.prepareStatement(deleteInventorySql);
         PreparedStatement psProduct = conn.prepareStatement(deleteProductSql)) {

        psInventory.setInt(1, idno);
        psInventory.executeUpdate();

        psProduct.setInt(1, idno);

        return psProduct.executeUpdate() > 0;

    } catch (SQLException e) {
        e.printStackTrace();
    }
    return false;
}
    
// added
public boolean deleteMultiple(List<Integer> ids) {

    if (ids == null || ids.isEmpty()) {
        return false;
    }

    StringBuilder placeholders = new StringBuilder();

    for (int i = 0; i < ids.size(); i++) {
        placeholders.append(i == 0 ? "?" : ",?");
    }

    String deleteInventorySql =
            "DELETE FROM inventory WHERE product_id IN (" + placeholders + ")";
    // ADDED
    String deleteProductSql =
            "DELETE FROM product WHERE idno IN (" + placeholders + ")";

    try (Connection conn = DBConnection.getConnection();
         PreparedStatement psInventory = conn.prepareStatement(deleteInventorySql);
         PreparedStatement psProduct = conn.prepareStatement(deleteProductSql)) {

        for (int i = 0; i < ids.size(); i++) {
            psInventory.setInt(i + 1, ids.get(i));
        }
        
        // ADDED
        for (int i = 0; i < ids.size(); i++) {
            psProduct.setInt(i + 1, ids.get(i));
        }

        psInventory.executeUpdate();
        return psProduct.executeUpdate() > 0; // ADDED

    } catch (SQLException e) {
        e.printStackTrace();
    }
    return false;
}

    /** SUM(price * stock) across every product - feeds the "Total Inventory Value" card. */
    public BigDecimal getTotalInventoryValue() {
        String sql = "SELECT COALESCE(SUM(price * stock), 0) FROM product";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }

    /** % of products whose stock is above lowStockThreshold - feeds the "Stock Health" gauge. */
    public double getStockHealthPercentage(int lowStockThreshold) {
        String sql = "SELECT SUM(CASE WHEN stock > ? THEN 1 ELSE 0 END) AS healthy, COUNT(*) AS total FROM product";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lowStockThreshold);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int total = rs.getInt("total");
                    if (total == 0) {
                        return 100.0;
                    }
                    return (rs.getInt("healthy") * 100.0) / total;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        return new Product(
                rs.getInt("idno"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("category"),
                rs.getString("brand"),
                rs.getBigDecimal("price"),
                rs.getInt("stock")
        );
    }
}
