package com.flop.resttester.request;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;

public class QueryParameterHandler {
    private final JTable table;
    private final DefaultTableModel model;

    public QueryParameterHandler(JTable table) {
        this.table = table;
        this.model = (DefaultTableModel) this.table.getModel();
        this.model.addRow(new String[]{"", ""});

        this.model.addTableModelListener(this::tableChanged);
    }

    public void tableChanged(TableModelEvent e) {
        int lastRow = this.model.getRowCount() - 1;

        String key = (String) this.model.getValueAt(lastRow, 0);
        String value = (String) this.model.getValueAt(lastRow, 1);

        if (e.getLastRow() == lastRow) {
            if (!key.isEmpty() || !value.isEmpty()) {
                this.model.addRow(new String[]{"", ""});
            }
        }

        if (e.getLastRow() == this.model.getRowCount() && e.getFirstRow() == e.getLastRow() && key.isEmpty() && value.isEmpty()) {
            // last row update event
            return;
        }
    }

    public List<QueryParam> getParams(){
        List<QueryParam> params = new ArrayList<>();

        for(int i = 0; i < this.model.getRowCount(); i++) {
            String key = this.model.getValueAt(i, 0).toString();
            String value = this.model.getValueAt(i, 1).toString();

            if(!key.isEmpty() && !value.isEmpty()) {
                params.add(new QueryParam(key, value));
            }
        }
        return params;
    }

    public void loadParams(List<QueryParam> params) {
        this.model.setColumnCount(0);

        for (QueryParam param : params) {
            this.model.addRow(new String[]{param.key, param.value});
        }
    }
}
