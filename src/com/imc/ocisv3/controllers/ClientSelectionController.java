package com.imc.ocisv3.controllers;

import com.imc.ocisv3.tools.Libs;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.*;
import org.zkoss.zul.event.PagingEvent;

import java.util.List;

/**
 * Created by faizal on 10/28/13.
 */
public class ClientSelectionController extends Window {

    private Logger log = LoggerFactory.getLogger(ClientSelectionController.class);
    private Listbox lbInactive;
    private Listbox lbActive;
    private Paging pgInactive;
    private Paging pgActive;
    private Tabbox tbx;
    private String where;

    public void onCreate() {
        if (Executions.getCurrent().getSession().getAttribute("u")!=null) {
            initComponents();
            initComponents();
            populateInactive(0, pgInactive.getPageSize());
            populateActive(0, pgActive.getPageSize());
        } else {
            Executions.getCurrent().getSession().setMaxInactiveInterval(0);
            Executions.getCurrent().getSession().invalidate();
            Executions.sendRedirect("index.zul");
        }
    }

    private void initComponents() {
        lbInactive = (Listbox) getFellow("lbInactive");
        lbActive = (Listbox) getFellow("lbActive");
        pgInactive = (Paging) getFellow("pgInactive");
        pgActive = (Paging) getFellow("pgActive");
        tbx = (Tabbox) getFellow("tbx");

        pgInactive.addEventListener("onPaging", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                PagingEvent evt = (PagingEvent) event;
                populateInactive(evt.getActivePage()*pgInactive.getPageSize(), pgInactive.getPageSize());
            }
        });
        pgActive.addEventListener("onPaging", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                PagingEvent evt = (PagingEvent) event;
                populateActive(evt.getActivePage()*pgActive.getPageSize(), pgActive.getPageSize());
            }
        });
    }

    private void populateInactive(int offset, int limit) {
        lbInactive.getItems().clear();
        Session s = Libs.sfEDC.openSession();
        try {
            String countQry = "select count(*) from ocis.dbo.cis_inslf where hinsid not in ('00002', '00003', '00004', '00012', '00018', '00022', '00029', '00034', '00036', '00037', '00040', '00042', '00044', '00046', '00050', '00051', '00052', '00053', '00054', '00059', '00061', '00062', '00063', '00064', '00067', '00068', '00070', '00071', '00072', '00073', '00075', '00076', '00077', '00078', '00079', '00080') ";
            String qry = "select hinsid, hinsname from ocis.dbo.cis_inslf where hinsid not in ('00002', '00003', '00004', '00012', '00018', '00022', '00029', '00034', '00036', '00037', '00040', '00042', '00044', '00046', '00050', '00051', '00052', '00053', '00054', '00059', '00061', '00062', '00063', '00064', '00067', '00068', '00070', '00071', '00072', '00073', '00075', '00076', '00077', '00078', '00079', '00080') ";

            if (where!=null) {
                countQry += "and (" + where + ") ";
                qry += "and (" + where + ") ";
            }

            qry += "order by hinsname asc";

            Integer count = (Integer) s.createSQLQuery(countQry).uniqueResult();
            pgInactive.setTotalSize(count);

            List<Object[]> l = s.createSQLQuery(qry).setFirstResult(offset).setMaxResults(limit).list();
            for (Object[] o : l) {
                String clientName = Libs.nn(o[1]).trim();

                Listitem li = new Listitem();
                li.setValue(o[0]);

                if (Libs.config.get("demo_mode").equals("true") && clientName.contains("REDPATH")) clientName = Libs.nn(Libs.config.get("demo_name"));
                li.appendChild(new Listcell(clientName));

                lbInactive.appendChild(li);
            }

        } catch (Exception ex) {
            log.error("populateInactive", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }
    }

    private void populateActive(int offset, int limit) {
        lbActive.getItems().clear();
        Session s = Libs.sfDB.openSession();
        try {
            String countQry = "select count(*) from idnhltpf.dbo.hltins where hinsid in ('00002', '00003', '00004', '00012', '00018', '00022', '00029', '00034', '00036', '00037', '00040', '00042', '00044', '00046', '00050', '00051', '00052', '00053', '00054', '00059', '00061', '00062', '00063', '00064', '00067', '00068', '00070', '00071', '00072', '00073', '00075', '00076', '00077', '00078', '00079', '00080') ";
            String qry = "select hinsid, hinsname from idnhltpf.dbo.hltins where hinsid in ('00002', '00003', '00004', '00012', '00018', '00022', '00029', '00034', '00036', '00037', '00040', '00042', '00044', '00046', '00050', '00051', '00052', '00053', '00054', '00059', '00061', '00062', '00063', '00064', '00067', '00068', '00070', '00071', '00072', '00073', '00075', '00076', '00077', '00078', '00079', '00080') ";

            if (where!=null) {
                countQry += "and (" + where + ") ";
                qry += "and (" + where + ") ";
            }

            qry += "order by hinsname asc";

            Integer count = (Integer) s.createSQLQuery(countQry).uniqueResult();
            pgActive.setTotalSize(count);

            List<Object[]> l = s.createSQLQuery(qry).setFirstResult(offset).setMaxResults(limit).list();
            for (Object[] o : l) {
                String clientName = Libs.nn(o[1]).trim();

                Listitem li = new Listitem();
                li.setValue(o[0]);

                if (Libs.config.get("demo_mode").equals("true") && clientName.contains("REDPATH")) clientName = Libs.nn(Libs.config.get("demo_name"));
                li.appendChild(new Listcell(clientName));

                lbActive.appendChild(li);
            }

        } catch (Exception ex) {
            log.error("populateActive", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }
    }

    public void refresh() {
        where = null;
        populateInactive(0, pgInactive.getPageSize());
        populateActive(0, pgActive.getPageSize());
    }

    public void inactiveClientSelected() {
        Libs.insuranceId = lbInactive.getSelectedItem().getValue().toString();
        Executions.sendRedirect("../main.zul");
    }

    public void activeClientSelected() {
        Libs.insuranceId = lbActive.getSelectedItem().getValue().toString();
        Executions.sendRedirect("../main.zul");
    }

    public void quickSearch() {
        String val = ((Textbox) getFellow("tQuickSearch")).getText();
        if (!val.isEmpty()) {
            where = "convert(varchar,hinsname) like '%" + val + "%' or "
                    + "hinsid like '%" + val + "%' ";

            if (tbx.getSelectedIndex()==1) {
                populateInactive(0, pgInactive.getPageSize());
            } else {
                populateActive(0, pgActive.getPageSize());
            }
        } else refresh();
    }

    public void logout() {
        if (Messagebox.show("Are you sure you want to logout?", "Confirmation", Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION)==Messagebox.OK) {
            Executions.getCurrent().getSession().setMaxInactiveInterval(0);
            Executions.getCurrent().getSession().invalidate();
            Executions.sendRedirect("/index.zul");
        }
    }

}
