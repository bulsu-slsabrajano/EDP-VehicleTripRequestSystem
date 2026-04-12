package com.admin.panel;

import com.project.dbConnection.DbConnectMsSql;
import com.project.util.CardPane;
import com.project.util.FxUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class UserPanel extends BorderPane {

    private final CardPane cards = new CardPane();
    private ObservableList<Object[]> tableData;
    private TableView<Object[]> table;
    private ComboBox<String> cmbRole, cmbStatus;
    private Connection conn;

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private ViewUserPanel   viewPanel;
    private UpdateUserPanel updatePanel;
    private DeleteUserPanel deletePanel;

    public UserPanel() {
        DbConnectMsSql db = new DbConnectMsSql();
        conn = db.conn;
        setBackground(Background.fill(Color.WHITE));

        viewPanel   = new ViewUserPanel();
        updatePanel = new UpdateUserPanel();
        deletePanel = new DeleteUserPanel();

        cards.addCard("LIST",        buildListPanel());
        cards.addCard("ADD",         new AddUserPanel());
        cards.addCard("VIEW",        viewPanel);
        cards.addCard("UPDATE",      updatePanel);
        cards.addCard("DEACTIVATE",  deletePanel);
        cards.show("LIST");
        setCenter(cards);
        loadUsers("All", "All");
    }

    // ── Cascade helpers ───────────────────────────────────────────────────────
    private void cascadeDriverNotAvailable(int driverId) throws Exception {
        PreparedStatement psFind = conn.prepareStatement(
            "SELECT assignment_id FROM Vehicle_Assignment WHERE driver_id=? AND assignment_status='Active'");
        psFind.setInt(1, driverId);
        ResultSet rs = psFind.executeQuery();
        List<Integer> ids = new ArrayList<>();
        while (rs.next()) ids.add(rs.getInt("assignment_id"));
        for (int aId : ids) {
            conn.prepareStatement(
                "UPDATE Vehicle_Assignment SET assignment_status='Inactive' WHERE assignment_id="+aId).executeUpdate();
            conn.prepareStatement(
                "UPDATE Trip SET trip_status='Pending' WHERE assignment_id="+aId+" AND trip_status='Approved'").executeUpdate();
        }
    }

    // ── List panel ────────────────────────────────────────────────────────────
    private BorderPane buildListPanel() {
        BorderPane main = new BorderPane();
        main.setBackground(Background.fill(Color.WHITE));
        main.setPadding(new Insets(20, 30, 20, 30));

        cmbRole   = FxUtil.styledCombo(FXCollections.observableArrayList("All","Admin","Driver","Passenger"));
        cmbRole.setValue("All"); cmbRole.setOnAction(e -> loadUsers(cmbRole.getValue(), cmbStatus.getValue()));
        cmbStatus = FxUtil.styledCombo(FXCollections.observableArrayList("All","Active","Inactive"));
        cmbStatus.setValue("All"); cmbStatus.setOnAction(e -> loadUsers(cmbRole.getValue(), cmbStatus.getValue()));

        Button btnRefresh = FxUtil.btnPrimary("Refresh");
        btnRefresh.setOnAction(e -> { cmbRole.setValue("All"); cmbStatus.setValue("All"); loadUsers("All","All"); });

        HBox topBar = new HBox(8,
            new Label("Filter by Role:"), cmbRole,
            FxUtil.hspacer(10), new Label("Account Status:"), cmbStatus,
            FxUtil.hgrow(), btnRefresh);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 10, 0));

        table = FxUtil.buildTable(
            "User ID","First Name","Middle Name","Last Name","Username","Email","Address","Role","Account Status");
        FxUtil.applyStatusRenderer(table, 8);
        tableData = FxUtil.tableData(table);

        Button btnView   = FxUtil.btnPrimary("View");
        Button btnAdd    = FxUtil.btnPrimary("Add");
        Button btnUpdate = FxUtil.btnPrimary("Update");
        Button btnDel    = FxUtil.btnDanger("Deactivate");

        btnAdd.setOnAction(e -> cards.show("ADD"));
        btnView.setOnAction(e -> {
            Object[] row = table.getSelectionModel().getSelectedItem();
            if (row == null) { FxUtil.showInfo(this, "Select a user first!"); return; }
            viewPanel.load((int)row[0], (String)row[7]); cards.show("VIEW");
        });
        btnUpdate.setOnAction(e -> {
            Object[] row = table.getSelectionModel().getSelectedItem();
            if (row == null) { FxUtil.showInfo(this, "Select a user first!"); return; }
            int id = (int) row[0];
            try {
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM Users WHERE user_id=?");
                ps.setInt(1, id); ResultSet rs = ps.executeQuery();
                if (rs.next()) updatePanel.setData(
                    rs.getInt("user_id"), rs.getString("first_name"), rs.getString("middle_name"),
                    rs.getString("last_name"), rs.getString("username"), rs.getString("email_address"),
                    rs.getString("address"), rs.getString("password"),
                    rs.getString("user_role"), rs.getString("user_status"));
            } catch (Exception ex) { ex.printStackTrace(); }
            cards.show("UPDATE");
        });
        btnDel.setOnAction(e -> {
            Object[] row = table.getSelectionModel().getSelectedItem();
            if (row == null) { FxUtil.showInfo(this, "Select a user first!"); return; }
            deletePanel.setDetails((int)row[0],(String)row[1],(String)row[2],(String)row[3],
                (String)row[4],(String)row[5],(String)row[6],(String)row[7],(String)row[8]);
            cards.show("DEACTIVATE");
        });

        HBox bottom = new HBox(8, btnView, btnAdd, btnUpdate, btnDel);
        bottom.setAlignment(Pos.CENTER_RIGHT);
        bottom.setPadding(new Insets(8, 0, 0, 0));

        main.setTop(topBar);
        main.setCenter(FxUtil.tableScroll(table));
        main.setBottom(bottom);
        return main;
    }

    private void loadUsers(String role, String status) {
        try {
            tableData.clear();
            boolean fr = !"All".equals(role), fs = !"All".equals(status);
            StringBuilder sql = new StringBuilder("SELECT * FROM Users WHERE 1=1");
            if (fr) sql.append(" AND user_role=?");
            if (fs) sql.append(" AND user_status=?");
            PreparedStatement ps = conn.prepareStatement(sql.toString());
            int idx = 1;
            if (fr) ps.setString(idx++, role);
            if (fs) ps.setString(idx, status);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tableData.add(new Object[]{
                    rs.getInt("user_id"), rs.getString("first_name"), rs.getString("middle_name"),
                    rs.getString("last_name"), rs.getString("username"), rs.getString("email_address"),
                    rs.getString("address"), rs.getString("user_role"), rs.getString("user_status")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── View User ─────────────────────────────────────────────────────────────
    class ViewUserPanel extends VBox {
        private Label lbId=vl(),lbFn=vl(),lbMn=vl(),lbLn=vl(),lbUn=vl(),lbEm=vl(),lbAd=vl(),lbRl=vl(),lbSt=vl();
        private Label lbLic=vl(), lbDs=vl();
        private Label lblLicKey=new Label("License No:"), lblDsKey=new Label("Driver Status:");
        private VBox contactsBox=new VBox(4);

        ViewUserPanel(){
            setBackground(Background.fill(Color.WHITE)); setAlignment(Pos.CENTER);
            VBox card=new VBox(0); card.getStyleClass().add("card"); card.setMaxWidth(580);
            Label title=new Label("User Details"); title.getStyleClass().add("title-medium");
            title.setStyle(title.getStyle()+"-fx-text-fill:#0096C7;");
            title.setMaxWidth(Double.MAX_VALUE); title.setAlignment(Pos.CENTER);
            GridPane grid=FxUtil.formGrid(); int y=0;
            FxUtil.addInfoRow(grid,"User ID:",       lbId, y++); FxUtil.addInfoRow(grid,"First Name:",  lbFn, y++);
            FxUtil.addInfoRow(grid,"Middle Name:",   lbMn, y++); FxUtil.addInfoRow(grid,"Last Name:",   lbLn, y++);
            FxUtil.addInfoRow(grid,"Username:",      lbUn, y++); FxUtil.addInfoRow(grid,"Email:",       lbEm, y++);
            FxUtil.addInfoRow(grid,"Address:",       lbAd, y++); FxUtil.addInfoRow(grid,"Role:",        lbRl, y++);
            FxUtil.addInfoRow(grid,"Account Status:",lbSt, y++);
            lblLicKey.getStyleClass().add("form-label"); lblDsKey.getStyleClass().add("form-label");
            lblLicKey.setVisible(false); lbLic.setVisible(false); lblDsKey.setVisible(false); lbDs.setVisible(false);
            grid.add(lblLicKey,0,y); grid.add(lbLic,1,y); y++;
            grid.add(lblDsKey,0,y);  grid.add(lbDs,1,y);  y++;
            GridPane.setMargin(lblLicKey,new Insets(6,10,6,10)); GridPane.setMargin(lbLic,new Insets(6,10,6,10));
            GridPane.setMargin(lblDsKey,new Insets(6,10,6,10));  GridPane.setMargin(lbDs,new Insets(6,10,6,10));
            Label ctitle=FxUtil.sectionLabel("Contact Numbers:");
            GridPane.setColumnSpan(ctitle,2); grid.add(ctitle,0,y++);
            GridPane.setColumnSpan(contactsBox,2); grid.add(contactsBox,0,y); GridPane.setMargin(contactsBox,new Insets(4,10,6,10));
            Button btnBack=FxUtil.btnOutlinePrimary("Back");
            btnBack.setOnAction(e->{cards.show("LIST");loadUsers("All","All");});
            HBox btnRow=new HBox(btnBack); btnRow.setAlignment(Pos.CENTER); btnRow.setPadding(new Insets(20,0,0,0));
            card.getChildren().addAll(title,FxUtil.spacer(20),grid,btnRow);
            ScrollPane sp=new ScrollPane(card); sp.setFitToWidth(true); sp.getStyleClass().add("edge-to-edge");
            getChildren().add(sp);
        }
        void load(int userId, String role){
            try{
                PreparedStatement ps=conn.prepareStatement("SELECT * FROM Users WHERE user_id=?");
                ps.setInt(1,userId); ResultSet rs=ps.executeQuery();
                if(rs.next()){
                    lbId.setText(String.valueOf(userId)); lbFn.setText(rs.getString("first_name"));
                    lbMn.setText(nvl(rs.getString("middle_name"),"—")); lbLn.setText(rs.getString("last_name"));
                    lbUn.setText(rs.getString("username")); lbEm.setText(rs.getString("email_address"));
                    lbAd.setText(nvl(rs.getString("address"),"—")); lbRl.setText(role);
                    String st=rs.getString("user_status"); lbSt.setText(st);
                    lbSt.setStyle("Active".equalsIgnoreCase(st)?"-fx-text-fill:#27AE60;-fx-font-weight:bold;":"-fx-text-fill:#DC3545;-fx-font-weight:bold;");
                }
                if("Driver".equalsIgnoreCase(role)){
                    PreparedStatement psD=conn.prepareStatement("SELECT license_number,driver_status FROM Driver WHERE driver_id=?");
                    psD.setInt(1,userId); ResultSet rsD=psD.executeQuery();
                    if(rsD.next()){
                        lbLic.setText(nvl(rsD.getString("license_number"),"—"));
                        String ds=rsD.getString("driver_status"); lbDs.setText(ds);
                        lbDs.setStyle("Available".equalsIgnoreCase(ds)?"-fx-text-fill:#27AE60;-fx-font-weight:bold;":"-fx-text-fill:#DC3545;-fx-font-weight:bold;");
                    }
                    lblLicKey.setVisible(true); lbLic.setVisible(true); lblDsKey.setVisible(true); lbDs.setVisible(true);
                } else {
                    lblLicKey.setVisible(false); lbLic.setVisible(false); lblDsKey.setVisible(false); lbDs.setVisible(false);
                }
                contactsBox.getChildren().clear();
                PreparedStatement psC=conn.prepareStatement("SELECT contact_number FROM User_Contact_Number WHERE user_id=?");
                psC.setInt(1,userId); ResultSet rsC=psC.executeQuery();
                boolean has=false;
                while(rsC.next()){has=true; Label l=new Label("• "+rsC.getString("contact_number")); l.getStyleClass().add("form-label"); contactsBox.getChildren().add(l);}
                if(!has){Label l=new Label("No contact numbers on record."); l.setStyle("-fx-text-fill:#888;"); contactsBox.getChildren().add(l);}
            }catch(Exception e){e.printStackTrace();}
        }
        private Label vl(){Label l=new Label();l.getStyleClass().add("form-label");return l;}
        private String nvl(String s,String def){return s!=null&&!s.isEmpty()?s:def;}
    }

    // ── Add User ──────────────────────────────────────────────────────────────
    class AddUserPanel extends VBox {
        private TextField fn=FxUtil.styledField(),mn=FxUtil.styledField(),ln=FxUtil.styledField();
        private TextField addr=FxUtil.styledField(),em=FxUtil.styledField(),un=FxUtil.styledField();
        private PasswordField pw=FxUtil.styledPasswordField();
        private TextField lic=FxUtil.styledField();
        private ComboBox<String> role=FxUtil.styledCombo(FXCollections.observableArrayList("Admin","Driver","Passenger"));
        private Label lblLicLbl=FxUtil.formLabel("License No:"), lblStatus=FxUtil.statusLabel();

        AddUserPanel(){
            setBackground(Background.fill(Color.WHITE)); setAlignment(Pos.CENTER);
            role.setValue("Admin"); lic.setVisible(false); lblLicLbl.setVisible(false);
            role.setOnAction(e->{boolean d="Driver".equals(role.getValue());lic.setVisible(d);lblLicLbl.setVisible(d);});
            VBox card=new VBox(0); card.getStyleClass().add("card"); card.setMaxWidth(580);
            Label title=new Label("Add User"); title.getStyleClass().add("title-medium");
            title.setMaxWidth(Double.MAX_VALUE); title.setAlignment(Pos.CENTER);
            GridPane form=FxUtil.formGrid(); int y=0;
            FxUtil.addFormRow(form,"First Name *:", fn,  y++); FxUtil.addFormRow(form,"Middle Name:", mn, y++);
            FxUtil.addFormRow(form,"Last Name *:",  ln,  y++); FxUtil.addFormRow(form,"Address:",     addr,y++);
            FxUtil.addFormRow(form,"Email *:",      em,  y++); FxUtil.addFormRow(form,"Username *:",  un,  y++);
            FxUtil.addFormRow(form,"Password *:",   pw,  y++); FxUtil.addFormRow(form,"Role *:",      role,y++);
            form.add(lblLicLbl,0,y); form.add(lic,1,y);
            GridPane.setMargin(lblLicLbl,new Insets(6,10,6,10)); GridPane.setMargin(lic,new Insets(6,10,6,10));
            Button btnAdd=FxUtil.btnPrimary("Add"), btnBack=FxUtil.btnOutlinePrimary("Back");
            btnAdd.setOnAction(e->save()); btnBack.setOnAction(e->{clear();cards.show("LIST");loadUsers("All","All");});
            HBox btnRow=new HBox(15,btnAdd,btnBack); btnRow.setAlignment(Pos.CENTER); btnRow.setPadding(new Insets(20,0,0,0));
            card.getChildren().addAll(title,FxUtil.spacer(20),form,btnRow,FxUtil.spacer(10),lblStatus);
            ScrollPane sp=new ScrollPane(card); sp.setFitToWidth(true); sp.getStyleClass().add("edge-to-edge");
            getChildren().add(sp);
        }
        private void save(){
            String r=role.getValue(), p=new String(pw.getText());
            String first=fn.getText().trim(), last=ln.getText().trim(), user=un.getText().trim(), email=em.getText().trim();
            if(first.isEmpty()){FxUtil.setError(lblStatus,"First name required!");return;}
            if(last.isEmpty()){FxUtil.setError(lblStatus,"Last name required!");return;}
            if(user.isEmpty()){FxUtil.setError(lblStatus,"Username required!");return;}
            if(p.isEmpty()){FxUtil.setError(lblStatus,"Password required!");return;}
            if(p.length()<8){FxUtil.setError(lblStatus,"Password must be ≥8 chars!");return;}
            if(email.isEmpty()){FxUtil.setError(lblStatus,"Email required!");return;}
            if(!EMAIL_PATTERN.matcher(email).matches()){FxUtil.setError(lblStatus,"Invalid email format!");return;}
            if("Driver".equals(r)&&!lic.getText().trim().isEmpty()){
                try{PreparedStatement c=conn.prepareStatement("SELECT COUNT(*) FROM Driver WHERE license_number=?");
                    c.setString(1,lic.getText().trim()); ResultSet rc=c.executeQuery();
                    if(rc.next()&&rc.getInt(1)>0){FxUtil.setError(lblStatus,"License number already exists!");return;}}
                catch(Exception e){e.printStackTrace();}
            }
            try{
                PreparedStatement cu=conn.prepareStatement("SELECT COUNT(*) FROM Users WHERE username=?");
                cu.setString(1,user); ResultSet ru=cu.executeQuery();
                if(ru.next()&&ru.getInt(1)>0){FxUtil.setError(lblStatus,"Username already exists!");return;}
                PreparedStatement ce=conn.prepareStatement("SELECT COUNT(*) FROM Users WHERE email_address=?");
                ce.setString(1,email); ResultSet re=ce.executeQuery();
                if(re.next()&&re.getInt(1)>0){FxUtil.setError(lblStatus,"Email already exists!");return;}
                PreparedStatement ps=conn.prepareStatement(
                    "INSERT INTO Users (first_name,middle_name,last_name,address,email_address,username,password,user_role,user_status) VALUES (?,?,?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
                ps.setString(1,first); ps.setString(2,mn.getText().trim().isEmpty()?null:mn.getText().trim());
                ps.setString(3,last);  ps.setString(4,addr.getText().trim().isEmpty()?null:addr.getText().trim());
                ps.setString(5,email); ps.setString(6,user); ps.setString(7,p); ps.setString(8,r); ps.setString(9,"Active");
                ps.executeUpdate();
                int newId=-1; ResultSet keys=ps.getGeneratedKeys(); if(keys.next()) newId=keys.getInt(1);
                if(newId==-1){FxUtil.setError(lblStatus,"Failed to get user ID!");return;}
                
            switch(r){
                case "Admin" -> {
                    PreparedStatement psAdmin = conn.prepareStatement("INSERT INTO Admin (admin_id) VALUES (?)");
                    psAdmin.setInt(1, newId);
                    psAdmin.executeUpdate();
                }
                case "Driver" -> {
                    PreparedStatement pd = conn.prepareStatement("INSERT INTO Driver (driver_id,license_number,driver_status) VALUES (?,?,?)");
                    pd.setInt(1, newId);
                    pd.setString(2, lic.getText().trim().isEmpty() ? null : lic.getText().trim());
                    pd.setString(3, "Available");
                    pd.executeUpdate();
                }
                case "Passenger" -> {
                    PreparedStatement psPass = conn.prepareStatement("INSERT INTO Passenger (passenger_id) VALUES (?)");
                    psPass.setInt(1, newId);
                    psPass.executeUpdate();
                }
            }
                FxUtil.setSuccess(lblStatus,"User added successfully!"); clear(); loadUsers("All","All");
            }catch(Exception e){FxUtil.setError(lblStatus,"Error: "+e.getMessage());e.printStackTrace();}
        }
        private void clear(){fn.clear();mn.clear();ln.clear();addr.clear();em.clear();un.clear();pw.clear();lic.clear();role.setValue("Admin");}
    }

    // ── Deactivate User ───────────────────────────────────────────────────────
    class DeleteUserPanel extends VBox {
        private int uid; private String urole;
        private Label lId=vl(),lFn=vl(),lMn=vl(),lLn=vl(),lUn=vl(),lEm=vl(),lAd=vl(),lRl=vl(),lSt=vl();
        private Label lblMsg=FxUtil.statusLabel();

        DeleteUserPanel(){
            setBackground(Background.fill(Color.WHITE)); setAlignment(Pos.CENTER);
            VBox card=new VBox(0); card.getStyleClass().add("card"); card.setMaxWidth(580);
            Label title=new Label("Deactivate User");
            title.setStyle("-fx-font-weight:bold;-fx-font-size:20px;-fx-text-fill:#DC3545;");
            title.setMaxWidth(Double.MAX_VALUE); title.setAlignment(Pos.CENTER);
            GridPane grid=FxUtil.formGrid(); int y=0;
            FxUtil.addInfoRow(grid,"User ID:",lId,y++);FxUtil.addInfoRow(grid,"First Name:",lFn,y++);
            FxUtil.addInfoRow(grid,"Middle Name:",lMn,y++);FxUtil.addInfoRow(grid,"Last Name:",lLn,y++);
            FxUtil.addInfoRow(grid,"Username:",lUn,y++);FxUtil.addInfoRow(grid,"Email:",lEm,y++);
            FxUtil.addInfoRow(grid,"Address:",lAd,y++);FxUtil.addInfoRow(grid,"Role:",lRl,y++);
            FxUtil.addInfoRow(grid,"Account Status:",lSt,y);
            Button btnConfirm=FxUtil.btnDanger("Confirm Deactivate"), btnBack=FxUtil.btnOutlinePrimary("Back");
            btnConfirm.setOnAction(e->deactivate()); btnBack.setOnAction(e->{cards.show("LIST");loadUsers("All","All");});
            HBox btnRow=new HBox(15,btnConfirm,btnBack); btnRow.setAlignment(Pos.CENTER); btnRow.setPadding(new Insets(20,0,0,0));
            card.getChildren().addAll(title,FxUtil.spacer(20),grid,btnRow,FxUtil.spacer(10),lblMsg);
            getChildren().add(card);
        }
        void setDetails(int id,String fn,String mn,String ln,String un,String em,String ad,String role,String st){
            uid=id; urole=role;
            lId.setText(String.valueOf(id)); lFn.setText(fn); lMn.setText(mn!=null?mn:"—");
            lLn.setText(ln); lUn.setText(un); lEm.setText(em); lAd.setText(ad!=null?ad:"—");
            lRl.setText(role); lSt.setText(st);
            lSt.setStyle("Active".equalsIgnoreCase(st)?"-fx-text-fill:#27AE60;-fx-font-weight:bold;":"-fx-text-fill:#DC3545;-fx-font-weight:bold;");
            lblMsg.setText(" ");
        }
        private void deactivate(){
            if(!FxUtil.confirm(this,"Set this user's account to Inactive?","Confirm Deactivate"))return;
            try{
                conn.prepareStatement("UPDATE Users SET user_status='Inactive' WHERE user_id="+uid).executeUpdate();
                if("Driver".equalsIgnoreCase(urole)){
                    conn.prepareStatement("UPDATE Driver SET driver_status='Not Available' WHERE driver_id="+uid).executeUpdate();
                    cascadeDriverNotAvailable(uid);
                }
                loadUsers("All","All"); cards.show("LIST");
            }catch(Exception e){FxUtil.setError(lblMsg,"Error deactivating user!");e.printStackTrace();}
        }
        private Label vl(){Label l=new Label();l.getStyleClass().add("form-label");return l;}
    }

    // ── Update User ───────────────────────────────────────────────────────────
    class UpdateUserPanel extends VBox {
        private int uid; private String curRole="", curStatus="", origLic="";
        private boolean pwdChanged=false;
        private TextField fn=FxUtil.styledField(),mn=FxUtil.styledField(),ln=FxUtil.styledField();
        private TextField addr=FxUtil.styledField(),em=FxUtil.styledField(),un=FxUtil.styledField();
        private PasswordField pw=FxUtil.styledPasswordField();
        private TextField lic=FxUtil.styledField();
        private ComboBox<String> role=FxUtil.styledCombo(FXCollections.observableArrayList("Admin","Driver","Passenger"));
        private ComboBox<String> status=FxUtil.styledCombo(FXCollections.observableArrayList("Active","Inactive"));
        private Label lblLicLbl=FxUtil.formLabel("License No:"), lblStatus=FxUtil.statusLabel();

        UpdateUserPanel(){
            setBackground(Background.fill(Color.WHITE)); setAlignment(Pos.CENTER);
            role.setValue("Admin"); status.setValue("Active"); lic.setVisible(false); lblLicLbl.setVisible(false);
            role.setOnAction(e->{boolean d="Driver".equals(role.getValue());lic.setVisible(d);lblLicLbl.setVisible(d);});
            pw.textProperty().addListener((o,ov,nv)->pwdChanged=true);
            VBox card=new VBox(0); card.getStyleClass().add("card"); card.setMaxWidth(580);
            Label title=new Label("Update User"); title.getStyleClass().add("title-medium");
            title.setMaxWidth(Double.MAX_VALUE); title.setAlignment(Pos.CENTER);
            GridPane form=FxUtil.formGrid(); int y=0;
            FxUtil.addFormRow(form,"First Name *:",fn,y++); FxUtil.addFormRow(form,"Middle Name:",mn,y++);
            FxUtil.addFormRow(form,"Last Name *:",ln,y++);  FxUtil.addFormRow(form,"Address:",addr,y++);
            FxUtil.addFormRow(form,"Email *:",em,y++);      FxUtil.addFormRow(form,"Username *:",un,y++);
            FxUtil.addFormRow(form,"Password:",pw,y++);     FxUtil.addFormRow(form,"Role *:",role,y++);
            FxUtil.addFormRow(form,"Account Status:",status,y++);
            form.add(lblLicLbl,0,y); form.add(lic,1,y);
            GridPane.setMargin(lblLicLbl,new Insets(6,10,6,10)); GridPane.setMargin(lic,new Insets(6,10,6,10));
            Button btnUpd=FxUtil.btnPrimary("Update"), btnBack=FxUtil.btnOutlinePrimary("Back");
            btnUpd.setOnAction(e->update()); btnBack.setOnAction(e->{lblStatus.setText(" ");cards.show("LIST");loadUsers("All","All");});
            HBox btnRow=new HBox(15,btnUpd,btnBack); btnRow.setAlignment(Pos.CENTER); btnRow.setPadding(new Insets(20,0,0,0));
            card.getChildren().addAll(title,FxUtil.spacer(20),form,btnRow,FxUtil.spacer(10),lblStatus);
            ScrollPane sp=new ScrollPane(card); sp.setFitToWidth(true); sp.getStyleClass().add("edge-to-edge");
            getChildren().add(sp);
        }
        void setData(int id,String first,String mid,String last,String user,String email,String addr2,String pwd,String r,String st){
            uid=id; curRole=r; curStatus=st!=null?st:"Active"; pwdChanged=false;
            fn.setText(nvl(first)); mn.setText(nvl(mid)); ln.setText(nvl(last));
            this.addr.setText(nvl(addr2)); em.setText(nvl(email)); un.setText(nvl(user));
            pw.setText(nvl(pwd)); role.setValue(r); status.setValue(curStatus);
            if("Driver".equals(r)){
                try{PreparedStatement ps=conn.prepareStatement("SELECT license_number FROM Driver WHERE driver_id=?");
                    ps.setInt(1,id); ResultSet rs=ps.executeQuery();
                    if(rs.next()){origLic=nvl(rs.getString("license_number")); lic.setText(origLic);}}
                catch(Exception e){e.printStackTrace();}
                lic.setVisible(true); lblLicLbl.setVisible(true);
            } else { lic.setVisible(false); lblLicLbl.setVisible(false); lic.clear(); origLic=""; }
            javafx.application.Platform.runLater(()->pwdChanged=false);
        }
        private void update(){
            String nr=role.getValue(), ns=status.getValue(), np=pw.getText(), email=em.getText().trim(), user=un.getText().trim();
            if(fn.getText().trim().isEmpty()){FxUtil.setError(lblStatus,"First name required!");return;}
            if(ln.getText().trim().isEmpty()){FxUtil.setError(lblStatus,"Last name required!");return;}
            if(user.isEmpty()){FxUtil.setError(lblStatus,"Username required!");return;}
            if(email.isEmpty()){FxUtil.setError(lblStatus,"Email required!");return;}
            if(!EMAIL_PATTERN.matcher(email).matches()){FxUtil.setError(lblStatus,"Invalid email format!");return;}
            if(pwdChanged&&!np.isEmpty()&&np.length()<8){FxUtil.setError(lblStatus,"Password must be ≥8 chars!");return;}
            if("Driver".equals(nr)&&lic.getText().trim().isEmpty()){FxUtil.setError(lblStatus,"License required for Driver!");return;}
            if("Driver".equals(nr)){
                String l=lic.getText().trim();
                if(!l.isEmpty()&&!l.equals(origLic)){
                    try{PreparedStatement c=conn.prepareStatement("SELECT COUNT(*) FROM Driver WHERE license_number=? AND driver_id<>?");
                        c.setString(1,l);c.setInt(2,uid); ResultSet rc=c.executeQuery();
                        if(rc.next()&&rc.getInt(1)>0){FxUtil.setError(lblStatus,"License number already exists!");return;}}
                    catch(Exception e){e.printStackTrace();}
                }
            }
            try{
                PreparedStatement cu=conn.prepareStatement("SELECT COUNT(*) FROM Users WHERE username=? AND user_id<>?");
                cu.setString(1,user);cu.setInt(2,uid); ResultSet ru=cu.executeQuery();
                if(ru.next()&&ru.getInt(1)>0){FxUtil.setError(lblStatus,"Username already taken!");return;}
                PreparedStatement ce=conn.prepareStatement("SELECT COUNT(*) FROM Users WHERE email_address=? AND user_id<>?");
                ce.setString(1,email);ce.setInt(2,uid); ResultSet re=ce.executeQuery();
                if(re.next()&&re.getInt(1)>0){FxUtil.setError(lblStatus,"Email already taken!");return;}
                PreparedStatement ps; String mnv=mn.getText().trim().isEmpty()?null:mn.getText().trim();
                String adv=addr.getText().trim().isEmpty()?null:addr.getText().trim();
                if(pwdChanged&&!np.trim().isEmpty()){
                    ps=conn.prepareStatement("UPDATE Users SET first_name=?,middle_name=?,last_name=?,address=?,email_address=?,username=?,password=?,user_role=?,user_status=? WHERE user_id=?");
                    ps.setString(1,fn.getText().trim());ps.setString(2,mnv);ps.setString(3,ln.getText().trim());ps.setString(4,adv);
                    ps.setString(5,email);ps.setString(6,user);ps.setString(7,np.trim());ps.setString(8,nr);ps.setString(9,ns);ps.setInt(10,uid);
                } else {
                    ps=conn.prepareStatement("UPDATE Users SET first_name=?,middle_name=?,last_name=?,address=?,email_address=?,username=?,user_role=?,user_status=? WHERE user_id=?");
                    ps.setString(1,fn.getText().trim());ps.setString(2,mnv);ps.setString(3,ln.getText().trim());ps.setString(4,adv);
                    ps.setString(5,email);ps.setString(6,user);ps.setString(7,nr);ps.setString(8,ns);ps.setInt(9,uid);
                }
                ps.executeUpdate();
                if("Active".equalsIgnoreCase(curStatus)&&"Inactive".equalsIgnoreCase(ns)&&"Driver".equalsIgnoreCase(nr)){
                    conn.prepareStatement("UPDATE Driver SET driver_status='Not Available' WHERE driver_id="+uid).executeUpdate();
                    cascadeDriverNotAvailable(uid);
                }
                if(!curRole.equals(nr)){
                    deleteFromRole(curRole,uid); insertIntoRole(nr,uid,lic.getText().trim());
                } else if("Driver".equals(nr)){
                    PreparedStatement pl=conn.prepareStatement("UPDATE Driver SET license_number=? WHERE driver_id=?");
                    pl.setString(1,lic.getText().trim().isEmpty()?null:lic.getText().trim()); pl.setInt(2,uid); pl.executeUpdate();
                    origLic=lic.getText().trim();
                }
                curRole=nr; curStatus=ns; pwdChanged=false;
                FxUtil.setSuccess(lblStatus,"User updated successfully!"); loadUsers("All","All");
            }catch(Exception e){FxUtil.setError(lblStatus,"Error: "+e.getMessage());e.printStackTrace();}
        }
        private void deleteFromRole(String r, int id) throws Exception {
            String sql = null;

            switch (r) {
                case "Admin":
                    sql = "DELETE FROM Admin WHERE admin_id=?";
                    break;
                case "Driver":
                    sql = "DELETE FROM Driver WHERE driver_id=?";
                    break;
                case "Passenger":
                    sql = "DELETE FROM Passenger WHERE passenger_id=?";
                    break;
            }

            if (sql != null) {
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        }
        
        private void insertIntoRole(String r,int id,String l) throws Exception{
            switch(r){
                case "Admin" -> conn.prepareStatement("INSERT INTO Admin (admin_id) VALUES ("+id+")").executeUpdate();
                case "Driver" -> {PreparedStatement pd=conn.prepareStatement("INSERT INTO Driver (driver_id,license_number,driver_status) VALUES (?,?,?)");
                    pd.setInt(1,id);pd.setString(2,l.isEmpty()?null:l);pd.setString(3,"Available");pd.executeUpdate();}
                case "Passenger" -> conn.prepareStatement("INSERT INTO Passenger (passenger_id) VALUES ("+id+")").executeUpdate();
            }
        }
        private String nvl(String s){return s!=null?s:"";}
    }

    private Label vl(){Label l=new Label();l.getStyleClass().add("form-label");return l;}
}