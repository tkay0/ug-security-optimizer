package org.ugoptimizer.ui.input;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds a consistent labeled-field form. Every team's create/edit screen
 * uses this instead of hand-laying-out its own form, so field spacing and
 * label style stay the same everywhere.
 */
public class InputReader {

    private final JPanel panel;
    private final Map<String, JComponent> fields = new LinkedHashMap<>();

    public InputReader() {
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    }

    public InputReader addTextField(String label) {
        addRow(label, new JTextField());
        return this;
    }

    public InputReader addDropdownField(String label, String[] options) {
        addRow(label, new JComboBox<>(options));
        return this;
    }

    public InputReader addCheckboxField(String label) {
        addRow(label, new JCheckBox());
        return this;
    }

    private void addRow(String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.add(new JLabel(label), BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        panel.add(row);
        fields.put(label, field);
    }

    /** Returns the current text (for text fields) or selected item (for dropdowns), trimmed. */
    public String getValue(String label) {
        JComponent field = fields.get(label);
        if (field instanceof JTextField textField) {
            return textField.getText().trim();
        }
        if (field instanceof JComboBox<?> comboBox) {
            Object selected = comboBox.getSelectedItem();
            return selected == null ? null : selected.toString();
        }
        return null;
    }

    /** Returns whether the named checkbox field is checked. */
    public boolean getChecked(String label) {
        JComponent field = fields.get(label);
        return field instanceof JCheckBox checkBox && checkBox.isSelected();
    }

    public void clear() {
        for (JComponent field : fields.values()) {
            if (field instanceof JTextField textField) {
                textField.setText("");
            } else if (field instanceof JComboBox<?> comboBox) {
                comboBox.setSelectedIndex(0);
            } else if (field instanceof JCheckBox checkBox) {
                checkBox.setSelected(false);
            }
        }
    }

    public JComponent getComponent() {
        return panel;
    }
}
