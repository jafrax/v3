package com.imc.ocisv3.controllers;

import com.imc.ocisv3.pojos.BenefitPOJO;
import com.imc.ocisv3.pojos.ClaimPOJO;
import com.imc.ocisv3.tools.Libs;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zul.*;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by faizal on 10/29/13.
 */
public class ClaimDetailController extends Window {

    private Logger log = LoggerFactory.getLogger(ClaimDetailController.class);
    private ClaimPOJO claimPOJO;
    private Listbox lb;

    public void onCreate() {
        System.out.println("created");
        claimPOJO = (ClaimPOJO) getAttribute("claim");

        initComponents();
        populate();
    }

    private void initComponents() {
        lb = (Listbox) getFellow("lb");

        getCaption().setLabel("Claim Detail [" + claimPOJO.getClaim_number() + "]");
    }

    private void populate() {
        populateInformation();
        populatePlanItems();
    }

    private void populateInformation() {
        Session s = Libs.sfDB.openSession();
        try {
            String qry = "select "
                    + "a.hclmsinyy, a.hclmsinmm, a.hclmsindd, "
                    + "a.hclmsoutyy, a.hclmsoutmm, a.hclmsoutdd, "
                    + "a.hclmdiscd1, a.hclmdiscd2, a.hclmdiscd3, "
                    + "b.hproname, a.hclmtclaim, c.hdt1name, "
                    + "c.hdt1bdtyy, c.hdt1bdtmm, c.hdt1bdtdd, "
                    + "c.hdt1sex, c.hdt1ncard, d.hhdrname, "
                    + "e.hmem2data1, e.hmem2data2, e.hmem2data3, e.hmem2data4 "
                    + "from idnhltpf.dbo.hltclm a "
                    + "inner join idnhltpf.dbo.hltpro b on b.hpronomor=a.hclmnhoscd "
                    + "inner join idnhltpf.dbo.hltdt1 c on c.hdt1yy=a.hclmyy and c.hdt1pono=a.hclmpono and c.hdt1idxno=a.hclmidxno and c.hdt1seqno=a.hclmseqno and c.hdt1ctr=0 "
                    + "inner join idnhltpf.dbo.hlthdr d on d.hhdryy=a.hclmyy and d.hhdrpono=a.hclmpono "
                    + "inner join idnhltpf.dbo.hltmemo2 e on e.hmem2yy=a.hclmyy and e.hmem2pono=a.hclmpono and e.hmem2idxno=a.hclmidxno and e.hmem2seqno=a.hclmseqno and e.hmem2claim=a.hclmtclaim and e.hmem2count=a.hclmcount "
                    + "where "
                    + "a.hclmcno='" + claimPOJO.getClaim_number() + "' and "
                    + "(convert(varchar,a.hclmyy)+'-'+convert(varchar,a.hclmbr)+'-'+convert(varchar,a.hclmdist)+'-'+convert(varchar,a.hclmpono))='" + claimPOJO.getPolicy_number() + "' and "
                    + "(convert(varchar,a.hclmidxno)+'-'+a.hclmseqno)='" + claimPOJO.getIndex() + "' and "
                    + "a.hclmcount=" + claimPOJO.getClaim_count();

            List<Object[]> l = s.createSQLQuery(qry).list();
            if (l.size()==1) {
                Object[] o = l.get(0);

                String diagnosis = Libs.nn(o[6]).trim();
                if (!Libs.nn(o[7]).trim().isEmpty()) diagnosis += ", " + Libs.nn(o[7]).trim();
                if (!Libs.nn(o[8]).trim().isEmpty()) diagnosis += ", " + Libs.nn(o[8]).trim();

                String dob = Libs.nn(o[12]) + "-" + Libs.nn(o[13]) + "-" + Libs.nn(o[14]);
                int ageDays = 0;
                try {
                    ageDays = Libs.getDiffDays(new SimpleDateFormat("yyyy-MM-dd").parse(dob), new Date());
                } catch (Exception ex) {
                    log.error("populateInformation", ex);
                }

                Label lAge = (Label) getFellow("lAge");
                lAge.setStyle("text-align:right;");
                lAge.setValue("(" + (ageDays / 365) + ")");

                String serviceIn = o[0] + "-" + o[1] + "-" + o[2];
                String serviceOut = o[3] + "-" + o[4] + "-" + o[5];

                ((Label) getFellow("lProvider")).setValue(Libs.nn(o[9]).trim());
                ((Label) getFellow("lDiagnosis")).setValue(diagnosis.toUpperCase());
                ((Label) getFellow("lDescription")).setValue(Libs.getICDByCode(diagnosis));
                ((Label) getFellow("lServiceIn")).setValue(serviceIn);
                ((Label) getFellow("lServiceOut")).setValue(serviceOut);
                ((Label) getFellow("lClaimType")).setValue(Libs.nn(o[10]).trim());
                ((Label) getFellow("lName")).setValue(Libs.nn(o[11]).trim());
                ((Label) getFellow("lDOB")).setValue(dob);
                ((Label) getFellow("lSex")).setValue(Libs.nn(o[15]).trim());
                ((Label) getFellow("lCardNumber")).setValue(Libs.nn(o[16]).trim());
                ((Label) getFellow("lCompanyName")).setValue(Libs.nn(o[17]).trim());
                ((Label) getFellow("lServiceDays")).setValue(String.valueOf(Libs.getDiffDays(new SimpleDateFormat("yyyy-MM-dd").parse(serviceIn), new SimpleDateFormat("yyyy-MM-dd").parse(serviceOut))));
                ((Label) getFellow("tRemarks")).setValue(Libs.nn(o[18]).trim() + Libs.nn(o[19]).trim() + "\n" + Libs.nn(o[20]).trim() + Libs.nn(o[21]).trim());
            }
        } catch (Exception ex) {
            log.error("populateInformation", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }
    }

    private void populatePlanItems() {
        lb.getItems().clear();
        Session s = Libs.sfDB.openSession();
        try {
            String qry = "select "
                    + "(a.hclmpcode1 + a.hclmpcode2) as plan_code, "
                    + Libs.createListFieldString("a.hclmcamt") + ", "
                    + Libs.createListFieldString("a.hclmaamt") + " "
                    + "from idnhltpf.dbo.hltclm a "
                    + "where "
                    + "a.hclmcno='" + claimPOJO.getClaim_number() + "' and "
                    + "(convert(varchar,a.hclmyy)+'-'+convert(varchar,a.hclmbr)+'-'+convert(varchar,a.hclmdist)+'-'+convert(varchar,a.hclmpono))='" + claimPOJO.getPolicy_number() + "' and "
                    + "(convert(varchar,a.hclmidxno)+'-'+a.hclmseqno)='" + claimPOJO.getIndex() + "' and "
                    + "a.hclmcount=" + claimPOJO.getClaim_count();

            double totalProposed = 0;
            double totalApproved = 0;

            List<Object[]> l = s.createSQLQuery(qry).list();
            if (l.size()==1) {
                Object[] o = l.get(0);

                BenefitPOJO benefitPOJO = Libs.getBenefit(claimPOJO.getPolicy_number(), Libs.nn(o[0]));

                int i=0;
                for (String planItem : benefitPOJO.getPlan_items()) {
                    if (!planItem.trim().isEmpty() && Double.valueOf(Libs.nn(o[i+1]))>0) {
                        totalProposed += Double.valueOf(Libs.nn(o[i+1]));
                        totalApproved += Double.valueOf(Libs.nn(o[i+31]));

                        Listitem li = new Listitem();
                        li.appendChild(new Listcell(Libs.getBenefitItemDescription(planItem)));
                        li.appendChild(Libs.createNumericListcell(Double.valueOf(Libs.nn(o[i+1])), "#,###.##"));
                        li.appendChild(Libs.createNumericListcell(Double.valueOf(Libs.nn(o[i+31])), "#,###.##"));
                        li.appendChild(Libs.createNumericListcell(Double.valueOf(Libs.nn(o[i+1]))-Double.valueOf(Libs.nn(o[i+31])), "#,###.##"));
                        li.appendChild(new Listcell(""));
                        lb.appendChild(li);
                    }
                    i++;
                }
            }

            ((Listfooter) getFellow("ftrTotalProposed")).setLabel(new DecimalFormat("#,###.##").format(totalProposed));
            ((Listfooter) getFellow("ftrTotalApproved")).setLabel(new DecimalFormat("#,###.##").format(totalApproved));
            ((Listfooter) getFellow("ftrTotalExcess")).setLabel(new DecimalFormat("#,###.##").format(totalProposed-totalApproved));

        } catch (Exception ex) {
            log.error("populatePlanItems", ex);
        } finally {
            if (s!=null && s.isOpen()) s.close();
        }
    }

}
