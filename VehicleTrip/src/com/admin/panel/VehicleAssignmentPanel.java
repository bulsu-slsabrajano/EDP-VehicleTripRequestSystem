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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class VehicleAssignmentPanel extends BorderPane {

    private final CardPane cards = new CardPane();
    private ObservableList<Object[]> tableData;
    private TableView<Object[]> table;
    private ComboBox<String> cmbFilter;
    private Connection conn;
    private int loggedInAdminId = -1;

    private AssignPanel       assignPanel;
    private UpdateAssignPanel updatePanel;
    private CancelAssignPanel cancelPanel;

    public VehicleAssignmentPanel() {
        DbConnectMsSql db = new DbConnectMsSql();
        conn = db.conn;
        setBackground(Background.fill(Color.WHITE));

        assignPanel  = new AssignPanel();
        updatePanel  = new UpdateAssignPanel();
        cancelPanel  = new CancelAssignPanel();

        cards.addCard("LIST",   buildListPanel());
        cards.addCard("ASSIGN", assignPanel);
        cards.addCard("UPDATE", updatePanel);
        cards.addCard("CANCEL", cancelPanel);
        cards.show("LIST");
        setCenter(cards);
        loadAssignments("All");
    }

    public void setLoggedInAdminId(int id) {
        loggedInAdminId = id;
        assignPanel.setAdminId(id);
        updatePanel.setAdminId(id);
    }

    private void cascadeAssignmentInactive(int assignmentId) throws Exception {
        PreparedStatement ps = conn.prepareStatement(
            "UPDATE Trip SET trip_status='Pending' WHERE assignment_id=? AND trip_status='Approved'");
        ps.setInt(1, assignmentId);
        ps.executeUpdate();
    }

    private BorderPane buildListPanel() {
        BorderPane main = new BorderPane();
        main.setBackground(Background.fill(Color.WHITE));
        main.setPadding(new Insets(20, 30, 20, 30));

        cmbFilter = FxUtil.styledCombo(FXCollections.observableArrayList("All","Active","Inactive"));
        cmbFilter.setValue("All");
        cmbFilter.setOnAction(e -> loadAssignments(cmbFilter.getValue()));
        Button btnRefresh = FxUtil.btnPrimary("Refresh");
        btnRefresh.setOnAction(e -> { cmbFilter.setValue("All"); loadAssignments("All"); });

        HBox topBar = new HBox(8, new Label("Filter by Status:"), cmbFilter, FxUtil.hgrow(), btnRefresh);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 10, 0));

        table = FxUtil.buildTable("Assignment ID","Driver","Vehicle","Admin","Date Assigned","Status");
        FxUtil.applyStatusRenderer(table, 5);
        tableData = FxUtil.tableData(table);

        Button btnAssign = FxUtil.btnPrimary("Assign");
        Button btnUpdate = FxUtil.btnPrimary("Update");
        Button btnCancel = FxUtil.btnDanger("Cancel Assignment");

        btnAssign.setOnAction(e -> { assignPanel.resetFields(); cards.show("ASSIGN"); });
        btnUpdate.setOnAction(e -> {
            Object[] row = table.getSelectionModel().getSelectedItem();
            if (row == null) { FxUtil.showInfo(this, "Select an assignment first!"); return; }
            updatePanel.load((int) row[0]); cards.show("UPDATE");
        });
        btnCancel.setOnAction(e -> {
            Object[] row = table.getSelectionModel().getSelectedItem();
            if (row == null) { FxUtil.showInfo(this, "Select an assignment first!"); return; }
            cancelPanel.setDetails((int)row[0],(String)row[1],(String)row[2],
                (String)row[3],String.valueOf(row[4]),(String)row[5]);
            cards.show("CANCEL");
        });

        HBox bottom = new HBox(8, btnAssign, btnUpdate, btnCancel);
        bottom.setAlignment(Pos.CENTER_RIGHT);
        bottom.setPadding(new Insets(8, 0, 0, 0));

        main.setTop(topBar);
        main.setCenter(FxUtil.tableScroll(table));
        main.setBottom(bottom);
        return main;
    }

    private void loadAssignments(String filter) {
        try {
            tableData.clear();
            String sql = "SELECT assignment_id,driver,vehicle,admin,date_assigned,assignment_status FROM vw_VehicleAssignments";
            if (!"All".equals(filter)) sql += " WHERE LOWER(assignment_status)=LOWER(?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            if (!"All".equals(filter)) ps.setString(1, filter);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tableData.add(new Object[]{
                    rs.getInt("assignment_id"), rs.getString("driver"),
                    rs.getString("vehicle"), rs.getString("admin"),
                    rs.getDate("date_assigned"), rs.getString("assignment_status")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Assign Panel ──────────────────────────────────────────────────────────
    class AssignPanel extends VBox {
        private ComboBox<String> cmbDriver = new ComboBox<>(), cmbVehicle = new ComboBox<>();
        private ComboBox<String> cmbStatus = FxUtil.styledCombo(FXCollections.observableArrayList("Active","Inactive"));
        private TextField txtDate  = FxUtil.styledField(), txtAdmin = FxUtil.readonlyField();
        private Label lblStatus    = FxUtil.statusLabel();
        private int[] driverIds = new int[0];
        private int[] vehicleIds = new int[0];
        private int adminId_ = -1;

        AssignPanel() {
            setBackground(Background.fill(Color.WHITE)); setAlignment(Pos.CENTER);
            cmbDriver.getStyleClass().add("combo-field"); cmbDriver.setPrefWidth(260);
            cmbVehicle.getStyleClass().add("combo-field"); cmbVehicle.setPrefWidth(260);
            cmbStatus.setValue("Active");
            VBox card = new VBox(0); card.getStyleClass().add("card"); card.setMaxWidth(520);
            Label title = new Label("Assign Vehicle");
            title.getStyleClass().add("title-medium"); title.setMaxWidth(Double.MAX_VALUE); title.setAlignment(Pos.CENTER);
            GridPane form = FxUtil.formGrid(); int y=0;
            FxUtil.addFormRow(form,"Driver:",       cmbDriver,  y++);
            FxUtil.addFormRow(form,"Vehicle:",      cmbVehicle, y++);
            FxUtil.addFormRow(form,"Admin:",        txtAdmin,   y++);
            FxUtil.addFormRow(form,"Date Assigned:",txtDate,    y++);
            FxUtil.addFormRow(form,"Status:",       cmbStatus,  y);
            Button btnSave=FxUtil.btnPrimary("Save"), btnBack=FxUtil.btnOutlinePrimary("Back");
            btnSave.setOnAction(e->save()); btnBack.setOnAction(e->{cards.show("LIST");loadAssignments("All");});
            HBox btnRow=new HBox(15,btnSave,btnBack); btnRow.setAlignment(Pos.CENTER); btnRow.setPadding(new Insets(20,0,0,0));
            card.getChildren().addAll(title,FxUtil.spacer(20),form,btnRow,FxUtil.spacer(10),lblStatus);
            getChildren().add(card);
        }
        void setAdminId(int id){ adminId_=id; loadAdminName(id); }
        private void loadAdminName(int id){
            try{
                PreparedStatement ps=conn.prepareStatement("SELECT first_name+' '+last_name AS n FROM Users WHERE user_id=?");
                ps.setInt(1,id); ResultSet rs=ps.executeQuery();
                if(rs.next()) txtAdmin.setText(rs.getString("n"));
            }catch(Exception e){e.printStackTrace();}
        }
        void resetFields(){
            loadDropdowns();
            txtDate.setText(new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()));
            cmbStatus.setValue("Active"); lblStatus.setText(" ");
            if(adminId_!=-1) loadAdminName(adminId_);
        }
        private void loadDropdowns(){
            try{
                cmbDriver.getItems().clear(); List<Integer> dIds=new ArrayList<>();
                PreparedStatement psD=conn.prepareStatement(
                    "SELECT d.driver_id, u.first_name+' '+u.last_name AS name FROM Driver d JOIN Users u ON d.driver_id=u.user_id WHERE d.driver_status='Available'");
                ResultSet rsD=psD.executeQuery();
                while(rsD.next()){cmbDriver.getItems().add(rsD.getString("name"));dIds.add(rsD.getInt("driver_id"));}
                driverIds=dIds.stream().mapToInt(i->i).toArray();
                cmbVehicle.getItems().clear(); List<Integer> vIds=new ArrayList<>();
                PreparedStatement psV=conn.prepareStatement(
                    "SELECT vehicle_id, vehicle_model+'  ('+plate_number+')' AS label FROM Vehicle WHERE vehicle_status='Available'");
                ResultSet rsV=psV.executeQuery();
                while(rsV.next()){cmbVehicle.getItems().add(rsV.getString("label"));vIds.add(rsV.getInt("vehicle_id"));}
                vehicleIds=vIds.stream().mapToInt(i->i).toArray();
            }catch(Exception e){e.printStackTrace();}
        }
        private void save(){
            if(cmbDriver.getItems().isEmpty()){FxUtil.setError(lblStatus,"No available drivers!");return;}
            if(cmbVehicle.getItems().isEmpty()){FxUtil.setError(lblStatus,"No available vehicles!");return;}
            if(adminId_==-1){FxUtil.setError(lblStatus,"Admin not set!");
            return;}
            java.sql.Date date;
            try{date=java.sql.Date.valueOf(txtDate.getText().trim());}
            catch(Exception ex){FxUtil.setError(lblStatus,"Invalid date! Use yyyy-MM-dd");return;}
            try{
                int dId=driverIds[cmbDriver.getSelectionModel().getSelectedIndex()];
                int vId=vehicleIds[cmbVehicle.getSelectionModel().getSelectedIndex()];
                
                PreparedStatement ps=conn.prepareStatement(
                    "INSERT INTO Vehicle_Assignment (driver_id,vehicle_id,admin_id,date_assigned,assignment_status) VALUES (?,?,?,?,?)");
                
                ps.setInt(1,dId);ps.setInt(2,vId);
                ps.setInt(3,adminId_);
                ps.setDate(4,date);ps.setString(5,cmbStatus.getValue());
                ps.executeUpdate();
                
                FxUtil.setSuccess(lblStatus,"Assignment saved successfully!");
                loadAssignments("All");
            }catch(Exception e){FxUtil.setError(lblStatus,"Error saving assignment!");e.printStackTrace();}
        }
    }

    // ── Update Assign Panel ───────────────────────────────────────────────────
    class UpdateAssignPanel extends VBox {
        private int assignId=-1; 
        private String prevStatus="";
        private ComboBox<String> cmbDriver=new ComboBox<>(), cmbVehicle=new ComboBox<>();
        private ComboBox<String> cmbStatus=FxUtil.styledCombo(FXCollections.observableArrayList("Active","Inactive"));
        private TextField txtDate=FxUtil.styledField(), txtAdmin=FxUtil.readonlyField();
        private Label lblStatus=FxUtil.statusLabel();
        private int[] driverIds = new int[0];
        private int[] vehicleIds = new int[0];
        private int adminId_ = -1;

        UpdateAssignPanel(){
            setBackground(Background.fill(Color.WHITE)); setAlignment(Pos.CENTER);
            cmbDriver.getStyleClass().add("combo-field"); cmbDriver.setPrefWidth(260);
            cmbVehicle.getStyleClass().add("combo-field"); cmbVehicle.setPrefWidth(260);
            VBox card=new VBox(0); card.getStyleClass().add("card"); card.setMaxWidth(520);
            Label title=new Label("Update Assignment");
            title.getStyleClass().add("title-medium"); title.setMaxWidth(Double.MAX_VALUE); title.setAlignment(Pos.CENTER);
            GridPane form=FxUtil.formGrid(); int y=0;
            FxUtil.addFormRow(form,"Driver:",       cmbDriver,  y++);
            FxUtil.addFormRow(form,"Vehicle:",      cmbVehicle, y++);
            FxUtil.addFormRow(form,"Admin:",        txtAdmin,   y++);
            FxUtil.addFormRow(form,"Date Assigned:",txtDate,    y++);
            FxUtil.addFormRow(form,"Status:",       cmbStatus,  y);
            Button btnUpd=FxUtil.btnPrimary("Update"), btnBack=FxUtil.btnOutlinePrimary("Back");
            btnUpd.setOnAction(e->update()); btnBack.setOnAction(e->{cards.show("LIST");loadAssignments("All");});
            HBox btnRow=new HBox(15,btnUpd,btnBack); btnRow.setAlignment(Pos.CENTER); btnRow.setPadding(new Insets(20,0,0,0));
            card.getChildren().addAll(title,FxUtil.spacer(20),form,btnRow,FxUtil.spacer(10),lblStatus);
            getChildren().add(card);
        }
        void setAdminId(int id) {
            this.adminId_ = id;
        }
        void load(int id){
            assignId=id; lblStatus.setText(" "); loadAllDropdowns();
            try{
                PreparedStatement ps=conn.prepareStatement(
                    "SELECT va.*,du.first_name+' '+du.last_name AS dname,v.vehicle_model+'  ('+v.plate_number+')' AS vlabel,au.first_name+' '+au.last_name AS aname " +
                    "FROM Vehicle_Assignment va JOIN Driver d ON va.driver_id=d.driver_id JOIN Users du ON d.driver_id=du.user_id " +
                    "JOIN Vehicle v ON va.vehicle_id=v.vehicle_id JOIN Admin a ON va.admin_id=a.admin_id JOIN Users au ON a.admin_id=au.user_id " +
                    "WHERE va.assignment_id=?");
                ps.setInt(1,id); ResultSet rs=ps.executeQuery();
                if(rs.next()){
                    int did=rs.getInt("driver_id"), vid=rs.getInt("vehicle_id");
                    for(int i=0;i<driverIds.length;i++) if(driverIds[i]==did){cmbDriver.getSelectionModel().select(i);break;}
                    for(int i=0;i<vehicleIds.length;i++) if(vehicleIds[i]==vid){cmbVehicle.getSelectionModel().select(i);break;}
                    txtDate.setText(rs.getDate("date_assigned").toString());
                    prevStatus=rs.getString("assignment_status"); cmbStatus.setValue(prevStatus);
                    txtAdmin.setText(rs.getString("aname"));
                }
            }catch(Exception e){e.printStackTrace();}
        }
        private void loadAllDropdowns(){
            try{
                cmbDriver.getItems().clear(); List<Integer> dIds=new ArrayList<>();
                ResultSet rsD=conn.prepareStatement("SELECT d.driver_id, u.first_name+' '+u.last_name AS name FROM Driver d JOIN Users u ON d.driver_id=u.user_id").executeQuery();
                while(rsD.next()){cmbDriver.getItems().add(rsD.getString("name"));dIds.add(rsD.getInt("driver_id"));}
                driverIds=dIds.stream().mapToInt(i->i).toArray();
                cmbVehicle.getItems().clear(); List<Integer> vIds=new ArrayList<>();
                ResultSet rsV=conn.prepareStatement("SELECT vehicle_id, vehicle_model+'  ('+plate_number+')' AS label FROM Vehicle").executeQuery();
                while(rsV.next()){cmbVehicle.getItems().add(rsV.getString("label"));vIds.add(rsV.getInt("vehicle_id"));}
                vehicleIds=vIds.stream().mapToInt(i->i).toArray();
            }catch(Exception e){e.printStackTrace();}
        }
        private void update(){
            if(assignId==-1) return;
            java.sql.Date date;
            try{date=java.sql.Date.valueOf(txtDate.getText().trim());}
            catch(Exception ex){FxUtil.setError(lblStatus,"Invalid date! Use yyyy-MM-dd");return;}
            String newStatus=cmbStatus.getValue();
            try{
                PreparedStatement ps=conn.prepareStatement(
                    "UPDATE Vehicle_Assignment SET driver_id=?,vehicle_id=?,date_assigned=?,assignment_status=? WHERE assignment_id=?");
                ps.setInt(1,driverIds[cmbDriver.getSelectionModel().getSelectedIndex()]);
                ps.setInt(2,vehicleIds[cmbVehicle.getSelectionModel().getSelectedIndex()]);
                ps.setDate(3,date); ps.setString(4,newStatus); ps.setInt(5,assignId);
                ps.executeUpdate();
                if("Active".equalsIgnoreCase(prevStatus)&&"Inactive".equalsIgnoreCase(newStatus))
                    cascadeAssignmentInactive(assignId);
                prevStatus=newStatus;
                FxUtil.setSuccess(lblStatus,"Assignment updated successfully!");
                loadAssignments("All");
            }catch(Exception e){FxUtil.setError(lblStatus,"Error updating assignment!");e.printStackTrace();}
        }
    }

    // ── Cancel Assign Panel ───────────────────────────────────────────────────
    class CancelAssignPanel extends VBox {
        private int assignId;
        private Label lblAid=vl(),lblDrv=vl(),lblVeh=vl(),lblAdm=vl(),lblDt=vl(),lblSt=vl();
        private Label lblMsg=FxUtil.statusLabel();
        CancelAssignPanel(){
            setBackground(Background.fill(Color.WHITE)); setAlignment(Pos.CENTER);
            VBox card=new VBox(0); card.getStyleClass().add("card"); card.setMaxWidth(520);
            Label title=new Label("Cancel Assignment");
            title.setStyle("-fx-font-weight:bold;-fx-font-size:20px;-fx-text-fill:#DC3545;");
            title.setMaxWidth(Double.MAX_VALUE); title.setAlignment(Pos.CENTER);
            GridPane grid=FxUtil.formGrid(); int y=0;
            FxUtil.addInfoRow(grid,"Assignment ID:",lblAid,y++); FxUtil.addInfoRow(grid,"Driver:",lblDrv,y++);
            FxUtil.addInfoRow(grid,"Vehicle:",lblVeh,y++); FxUtil.addInfoRow(grid,"Admin:",lblAdm,y++);
            FxUtil.addInfoRow(grid,"Date Assigned:",lblDt,y++); FxUtil.addInfoRow(grid,"Status:",lblSt,y);
            Label warn=new Label("Cancelling will set the assignment to Inactive\nand revert any Approved trips back to Pending.");
            warn.setStyle("-fx-font-style:italic;-fx-font-size:11px;-fx-text-fill:#B46400;");
            warn.setPadding(new Insets(8,0,8,0));
            Button btnConfirm=FxUtil.btnDanger("Confirm Cancel"), btnBack=FxUtil.btnOutlinePrimary("Back");
            btnConfirm.setOnAction(e->cancel()); btnBack.setOnAction(e->{cards.show("LIST");loadAssignments("All");});
            HBox btnRow=new HBox(15,btnConfirm,btnBack); btnRow.setAlignment(Pos.CENTER); btnRow.setPadding(new Insets(16,0,0,0));
            card.getChildren().addAll(title,FxUtil.spacer(20),grid,warn,btnRow,FxUtil.spacer(10),lblMsg);
            getChildren().add(card);
        }
        void setDetails(int id,String drv,String veh,String adm,String dt,String st){
            assignId=id; lblAid.setText(String.valueOf(id)); lblDrv.setText(drv); lblVeh.setText(veh);
            lblAdm.setText(adm); lblDt.setText(dt); lblSt.setText(st); lblMsg.setText(" ");
        }
        private void cancel(){
            if(!FxUtil.confirm(this,"Are you sure you want to cancel this assignment?\nApproved trips linked to it will revert to Pending.","Confirm Cancel"))return;
            try{
                conn.prepareStatement("UPDATE Vehicle_Assignment SET assignment_status='Inactive' WHERE assignment_id="+assignId).executeUpdate();
                cascadeAssignmentInactive(assignId);
                loadAssignments("All"); cards.show("LIST");
            }catch(Exception e){FxUtil.setError(lblMsg,"Error cancelling assignment!");e.printStackTrace();}
        }
        private Label vl(){Label l=new Label();l.getStyleClass().add("form-label");return l;}
    }
}