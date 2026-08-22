package org.ugoptimizer.ui.display;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.util.List;

/**
 * Reusable table shared by every screen in the application: any team can
 * display a list of rows without writing its own {@link AbstractTableModel}.
 */
public class DataTablePanel<T> extends JPanel {

    private final List<Column<T>> columns;
    private List<T> rows;
    private final RowTableModel tableModel;
    private final JTable table;

    public DataTablePanel(List<Column<T>> columns, List<T> rows) {
        super(new BorderLayout());
        this.columns = columns;
        this.rows = rows;
        this.tableModel = new RowTableModel();
        this.table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void setRows(List<T> rows) {
        this.rows = rows;
        tableModel.fireTableDataChanged();
    }

    /** Returns the selected row's underlying object, or {@code null} when nothing is selected. */
    public T getSelectedRow() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        return rows.get(table.convertRowIndexToModel(viewRow));
    }

    public JTable getTable() {
        return table;
    }

    private class RowTableModel extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.size();
        }

        @Override
        public String getColumnName(int columnIndex) {
            return columns.get(columnIndex).header();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return columns.get(columnIndex).extractor().apply(rows.get(rowIndex));
        }
    }
}
