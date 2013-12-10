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
 * Created by faizal on 10/25/13.
 */
public class InHospitalMonitoringController extends Window {

    private Logger log = LoggerFactory.getLogger(InHospitalMonitoringController.class);
    private Listbox lb;
    private Paging pg;
    private String where;

    public void onCreate() {
        initComponents();
        populate(0, pg.getPageSize());
    }

    private void initComponents() {
        lb = (Listbox) getFellow("lb");
        pg = (Paging) getFellow("pg");

        pg.addEventListener("onPaging", new EventListener() {
            @Override
            public void onEvent(Event event) throws Exception {
                PagingEvent evt = (PagingEvent) event;
                populate(evt.getActivePage()*pg.getPageSize(), pg.getPageSize());
            }
        });
    }

    private void populate(int offset, int limit) {
        lb.getItems().clear();

        Session s = Libs.sfDB.openSession();
        try {
            String countQry = "select count(*) "
                    + "from surjam_new.dbo.ms_surjam a "
                    + "inner join idnhltpf.dbo.hlthdr b on b.hhdryy=a.thn_polis and b.hhdrpono=a.no_polis "
                    + "inner join idnhltpf.dbo.hltdt1 c on c.hdt1yy=a.thn_polis and c.hdt1pono=a.no_polis and c.hdt1idxno=a.idx and c.hdt1seqno=a.seq and c.hdt1ctr=0 "
                    + "where "
                    + "b.hhdrinsid='" + Libs.insuranceId + "' ";

            String qry = "select "
                    + "a.nosurat, "
                    + "a.thn_polis, a.br_polis, a.dist_polis, a.no_polis, "
                    + "a.idx, a.seq, a.nokartu, "
                    + "a.nmpeserta, a.namars, "
                    + "a.kelas, a.kettrans, "
                    + "c.hdt1bdtyy, c.hdt1bdtmm, c.hdt1bdtdd, " // 12
                    + "c.hdt1sex, "
                    + "a.klskamar, a.hrgkamar, " // 16
                    + "a.tg_rawat, "
                    + "b.hhdrname, "
                    + "a.diagnosa, a.icd, " // 20
                    + "a.user_prv, tgl_print, "
                    + "a.tg_keluar, a.estimasi, a.notepenting, "
                    + "c.hdt1mstat " // 27
                    + "from surjam_new.dbo.ms_surjam a "
                    + "inner join idnhltpf.dbo.hlthdr b on b.hhdryy=a.thn_polis and b.hhdrpono=a.no_polis "
                    + "inner join idnhltpf.dbo.hltdt1 c on c.hdt1yy=a.thn_polis and c.hdt1pono=a.no_polis and c.hdt1idxno=a.idx and c.hdt1seqno=a.seq and c.hdt1ctr=0 "
                    + "where "
                    + "b.hhdrinsid='" + Libs.insuranceId + "' ";

            if (where!=null) {
                countQry += "and (" + where + ") ";
                qry += "and (" + where + ") ";
            }

            qry += "order by nosurat desc;";

            Integer count = (Integer) s.createSQLQuery(countQry).uniqueResult();
            pg.setTotalSize(count);

            List<Object[]> l = s.createSQLQuery(qry).setFirstResult(offset).setMaxResults(limit).list();
            for (Object[] o : l) {
                Listcell lcStatus = new Listcell();
                Label lStatus = new Label();
                if (Libs.nn(o[11]).trim().equals("0")) {
                    lStatus.setValue("CANCELED");
                } else if (Libs.nn(o[11]).trim().equals("1")) {
                    lStatus.setValue("ACTIVE");
                    lStatus.setStyle("color:#00FF00");
                } else if (Libs.nn(o[11]).trim().equals("2")) {
                    lStatus.setValue("CLOSED");
                    lStatus.setStyle("color:#FF0000;");
                }
                lcStatus.appendChild(lStatus);

                Listitem li = new Listitem();
                li.setValue(o);

                li.appendChild(new Listcell(Libs.nn(o[0])));
                li.appendChild(new Listcell(Libs.nn(o[1]) + "-" + Libs.nn(o[2]) + "-" + Libs.nn(o[3]) + "-" + Libs.nn(o[4])));
                li.appendChild(new Listcell(Libs.nn(o[7])));
                li.appendChild(new Listcell(Libs.nn(o[5]) + "-" + Libs.nn(o[6])));
                li.appendChild(new Listcell(Libs.nn(o[8]).trim()));
                li.appendChild(lcStatus);
                li.appendChild(new Listcell(Libs.nn(o[10]).trim()));
                li.appendChild(new Listcell(Libs.nn(o[9]).trim()));
                lb.appendChild(li);
            }
        } catch (Exception ex) {
            log.error("populate", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }
    }

    public void refresh() {
        where = null;
        populate(0, pg.getPageSize());
    }

    public void lbSelected() {}

    public void showInHospitalMonitoringDetail() {
        Window w = (Window) Executions.createComponents("views/InHospitalMonitoringDetail.zul", Libs.getRootWindow(), null);
        w.setAttribute("ihm", lb.getSelectedItem().getValue());
        w.doModal();
    }

    public void quickSearch() {
        String val = ((Textbox) getFellow("tQuickSearch")).getText();
        if (!val.isEmpty()) {
            where = "convert(varchar, a.nokartu) like '%" + val + "%' or "
                    + "a.nmpeserta like '%" + val + "%' or "
                    + "a.namars like '%" + val + "%' or "
                    + "(convert(varchar, a.idx) + '-' + a.seq)='" + val + "' ";

            populate(0, pg.getPageSize());
        } else refresh();
    }

    public void export() {
        Libs.showDeveloping();
    }

}
