package com.imc.ocisv3.controllers;

import com.imc.ocisv3.tools.Libs;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zhtml.Messagebox;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

/**
 * Created by faizal on 10/24/13.
 */
public class IndexController extends Window {

    private Logger log = LoggerFactory.getLogger(IndexController.class);
    private Textbox tUsername;
    private Textbox tPassword;

    public void onCreate() {
        initComponents();
    }

    private void initComponents() {
        tUsername = (Textbox) getFellow("tUsername");
        tPassword = (Textbox) getFellow("tPassword");
    }

    public void login() {
        Session s = Libs.sfEDC.openSession();
        try {
            String qry = "select "
                    + "fullname, ins_id, userlv "
                    + "from ocis.dbo.cis_user "
                    + "where "
                    + "user_id='" + tUsername.getText() + "' and "
                    + "pass='" + tPassword.getText() + "'";
            
            System.out.println(qry);

            List<Object[]> l = s.createSQLQuery(qry).list();
            if (l.size()==1) {
                Object[] o = l.get(0);

                Executions.getCurrent().getSession().setAttribute("u", tUsername.getText());
                Libs.userLevel = Integer.valueOf(Libs.nn(o[2]));

                Libs.log_login(tUsername.getText(), new Timestamp(new Date().getTime()));

                if (Libs.userLevel==1) {
                    Executions.sendRedirect("views/ClientSelection.zul");
                } else {
                    Libs.getSession().setAttribute("insuranceId", Libs.nn(o[1]));
                    Executions.sendRedirect("main.zul");
                }
            } else {
                Messagebox.show("Invalid Username/Password combination. This incident is reported.", "Error", Messagebox.OK, Messagebox.ERROR);
            }
        } catch (Exception ex) {
            log.error("login", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }
    }

}
