package com.imc.ocisv3.controllers;

import com.imc.ocisv3.tools.Libs;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.*;

import java.io.File;
import java.util.List;

/**
 * Created by faizal on 10/24/13.
 */
public class MainController extends Window {

    private Logger log = LoggerFactory.getLogger(MainController.class);
    private Tree tree;
    private Image imgCompanyLogo;

    public void onCreate() {
        if (Executions.getCurrent().getSession().getAttribute("u")!=null) {
            initComponents();
            getPolicies();
            open("Dashboard");
            setVisible(true);
        } else {
            Executions.getCurrent().getSession().setMaxInactiveInterval(0);
            Executions.getCurrent().getSession().invalidate();
            Executions.sendRedirect("index.zul");
        }
    }

    private void initComponents() {
        tree = (Tree) getFellow("tree");
        imgCompanyLogo = (Image) getFellow("imgCompanyLogo");

        Libs.getDesktop().setAttribute("rootWindow", this);
        Libs.getDesktop().setAttribute("center", getFellow("center"));

        String imageFile = "";

        if (Libs.config.get("demo_mode").equals("true") && Libs.insuranceId.equals("00051")) {
            imageFile = "resources/companies/99999.jpg";
        } else {
            imageFile = "resources/companies/" + Libs.insuranceId + ".jpg";
            File f = new File(Executions.getCurrent().getSession().getWebApp().getRealPath(imageFile));
            if (!f.exists()) imageFile = "resources/companies/00000.jpg";
        }

        imgCompanyLogo.setSrc(imageFile);

        if (Libs.userLevel==1) ((Treeitem) getFellow("tiClientSelection")).setVisible(true);
    }

    private void getPolicies() {
        Libs.policyMap.clear();
        Session s = Libs.sfDB.openSession();
        try {
            String qry = "select "
                    + "(convert(varchar,a.hhdryy)+'-'+convert(varchar,a.hhdrbr)+'-'+convert(varchar,a.hhdrdist)+'-'+convert(varchar,a.hhdrpono)) as policy, "
                    + "a.hhdrname "
                    + "from idnhltpf.dbo.hlthdr a "
                    + "where "
                    + "a.hhdrinsid='" + Libs.insuranceId + "' "
                    + "order by a.hhdrname asc ";

            List<Object[]> l = s.createSQLQuery(qry).list();
            for (Object[] o : l) {
                Libs.policyMap.put(Libs.nn(o[0]), Libs.nn(o[1]).trim());
            }
        } catch (Exception ex) {
            log.error("getPolicies", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }
    }

    public void open(String view) {
        if (Libs.getCenter().getChildren().size()>0) Libs.getCenter().removeChild(Libs.getCenter().getFirstChild());
        Executions.createComponents("views/" + view + ".zul", Libs.getCenter(), null);
    }

    public void logout() {
        if (Messagebox.show("Are you sure you want to logout?", "Confirmation", Messagebox.OK | Messagebox.CANCEL, Messagebox.QUESTION)==Messagebox.OK) {
            Executions.getCurrent().getSession().setMaxInactiveInterval(0);
            Executions.getCurrent().getSession().invalidate();
            Executions.sendRedirect("/index.zul");
        }
    }

    public void clientSelection() {
        Executions.sendRedirect("views/ClientSelection.zul");
    }

}
