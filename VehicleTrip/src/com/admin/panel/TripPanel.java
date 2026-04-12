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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TripPanel extends BorderPane {

	private final CardPane cards = new CardPane(); 
	private ObservableList<Object[]> tableData = FXCollections.observableArrayList(); 
	private TableView<Object[]> table; 
	private ComboBox<String> cmbFilter; 
    private Connection conn;
    private int loggedInAdminId = -1;
    private String loggedInAdminName = "";
    private final List<Integer> tripIds = new ArrayList<>();

    public CreateTripPanel createPanel;
    private UpdateTripPanel updatePanel;
    private ViewTripPanel   viewPanel;

    public TripPanel(int adminId) {
        DbConnectMsSql db = new DbConnectMsSql();
        conn = db.conn;
        loggedInAdminId   = adminId;
        loggedInAdminName = fetchAdminName(adminId);
        setBackground(Background.fill(Color.WHITE));

        createPanel = new CreateTripPanel();
        updatePanel = new UpdateTripPanel();
        viewPanel   = new ViewTripPanel();

        cards.addCard("LIST",   buildListPanel());
        cards.addCard("CREATE", createPanel);
        cards.addCard("UPDATE", updatePanel);
        cards.addCard("VIEW",   viewPanel);
        cards.show("LIST");
        setCenter(cards);
        loadTrips("All");
    }

    public TripPanel() { this(-1); }

    private String fetchAdminName(int id) {
        if (id <= 0) return "—";
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT first_name+' '+last_name AS n FROM Users WHERE user_id=?");
            ps.setInt(1, id); ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("n");
        } catch (Exception e) { e.printStackTrace(); }
        return "—";
    }

    private BorderPane buildListPanel() {
        BorderPane main = new BorderPane();
        main.setBackground(Background.fill(Color.WHITE));
        main.setPadding(new Insets(20, 30, 20, 30));

        cmbFilter = FxUtil.styledCombo(FXCollections.observableArrayList("All","Pending","Approved","Completed","Cancelled"));
        cmbFilter.setValue("All");
        cmbFilter.setOnAction(e -> loadTrips(cmbFilter.getValue()));
        Button btnRefresh = FxUtil.btnPrimary("Refresh");
        btnRefresh.setOnAction(e -> { cmbFilter.setValue("All"); loadTrips("All"); });

        HBox topBar = new HBox(8, new Label("Filter by Status:"), cmbFilter, FxUtil.hgrow(), btnRefresh);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 10, 0));

        table = (TableView<Object[]>) FxUtil.buildTable(
            "Trip ID","Passenger","Admin","Assignment",
            "Start Date","Start Time","End Date","End Time",
            "Pickup","Destination","Pax","Status");
        FxUtil.applyStatusRenderer(table, 11);
        tableData = (ObservableList<Object[]>) FxUtil.tableData(table);

        Button btnView    = FxUtil.btnPrimary("View");
        Button btnCreate  = FxUtil.btnPrimary("Create Trip");
        Button btnUpdate  = FxUtil.btnPrimary("Update");
        Button btnApprove = FxUtil.btnSuccess("Approve");
        Button btnReject  = FxUtil.btnDanger("Reject");

        btnView.setOnAction(e -> {
            int idx = table.getSelectionModel().getSelectedIndex();
            if (idx < 0) { FxUtil.showInfo(this,"Select a trip first!"); return; }
            viewPanel.load(tripIds.get(idx)); cards.show("VIEW");
        });
        btnCreate.setOnAction(e -> { createPanel.resetFields(); cards.show("CREATE"); });
        btnUpdate.setOnAction(e -> {
            int idx = table.getSelectionModel().getSelectedIndex();
            if (idx < 0) { FxUtil.showInfo(this,"Select a trip first!"); return; }
            updatePanel.load(tripIds.get(idx)); cards.show("UPDATE");
        });
        btnApprove.setOnAction(e -> {
            int idx = table.getSelectionModel().getSelectedIndex();
            if (idx < 0) { FxUtil.showInfo(this,"Select a trip first!"); return; }
            Object[] row = tableData.get(idx);
            if (!"Pending".equalsIgnoreCase((String)row[11])) { FxUtil.showInfo(this,"Only Pending trips can be approved."); return; }
            Object av = row[3];
            if (av == null || av.toString().equals("—") || av.toString().isEmpty()) {
                FxUtil.showWarning(this,"This trip has no assignment yet.\nPlease update it first."); return; }
            updateStatus(tripIds.get(idx), "Approved");
        });
        btnReject.setOnAction(e -> {
            int idx = table.getSelectionModel().getSelectedIndex();
            if (idx < 0) { FxUtil.showInfo(this,"Select a trip first!"); return; }
            if (!"Pending".equalsIgnoreCase((String)tableData.get(idx)[11])) { FxUtil.showInfo(this,"Only Pending trips can be rejected."); return; }
            if (FxUtil.confirm(this,"Reject this trip? It will be marked as Cancelled.","Confirm Reject"))
                updateStatus(tripIds.get(idx), "Cancelled");
        });

        HBox bottom = new HBox(8, btnView, btnCreate, btnUpdate, btnApprove, btnReject);
        bottom.setAlignment(Pos.CENTER_RIGHT);
        bottom.setPadding(new Insets(8, 0, 0, 0));

        main.setTop(topBar);
        main.setCenter(FxUtil.tableScroll(table));
        main.setBottom(bottom);
        return main;
    }

    private void updateStatus(int tripId, String s) {
        try {
            PreparedStatement ps = conn.prepareStatement("UPDATE Trip SET trip_status=? WHERE trip_id=?");
            ps.setString(1,s); ps.setInt(2,tripId); ps.executeUpdate();
            loadTrips(cmbFilter.getValue());
        } catch (Exception e) { e.printStackTrace(); }
    }

    void loadTrips(String filter) {
        try {
            tableData.clear(); tripIds.clear();
            String sql =
                "SELECT t.trip_id, u.first_name+' '+u.last_name AS passenger," +
                "ISNULL(au.first_name+' '+au.last_name,'—') AS admin_name," +
                "ISNULL(du.first_name+' '+du.last_name+' - '+v.vehicle_model,'—') AS assignment," +
                "t.start_date,t.start_time,t.end_date,t.end_time," +
                "t.pick_up_location,t.destination,t.passenger_count,t.trip_status " +
                "FROM Trip t JOIN Passenger p ON t.passenger_id=p.passenger_id JOIN Users u ON p.passenger_id=u.user_id " +
                "LEFT JOIN Admin a ON t.admin_id=a.admin_id LEFT JOIN Users au ON a.admin_id=au.user_id " +
                "LEFT JOIN Vehicle_Assignment va ON t.assignment_id=va.assignment_id " +
                "LEFT JOIN Driver d ON va.driver_id=d.driver_id LEFT JOIN Users du ON d.driver_id=du.user_id " +
                "LEFT JOIN Vehicle v ON va.vehicle_id=v.vehicle_id";
            if (!"All".equals(filter)) sql += " WHERE LOWER(t.trip_status)=LOWER(?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            if (!"All".equals(filter)) ps.setString(1, filter);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tripIds.add(rs.getInt("trip_id"));
                tableData.add(new Object[]{
                    rs.getInt("trip_id"), rs.getString("passenger"), rs.getString("admin_name"),
                    rs.getString("assignment"), rs.getObject("start_date"), rs.getObject("start_time"),
                    rs.getObject("end_date"), rs.getObject("end_time"),
                    rs.getString("pick_up_location"), rs.getString("destination"),
                    rs.getInt("passenger_count"), rs.getString("trip_status")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private List<Integer> availableAssignments(java.sql.Date start, java.sql.Date end, int excludeTrip) {
        List<Integer> available = new ArrayList<>();
        try {
            ResultSet rsAll = conn.prepareStatement(
                "SELECT assignment_id FROM Vehicle_Assignment WHERE assignment_status='Active'").executeQuery();
            List<Integer> all = new ArrayList<>();
            while (rsAll.next()) all.add(rsAll.getInt("assignment_id"));
            String blk = "SELECT DISTINCT assignment_id FROM Trip WHERE trip_status IN ('Pending','Approved','Completed')" +
                " AND assignment_id IS NOT NULL AND NOT (end_date < ? OR start_date > ?)";
            if (excludeTrip > 0) blk += " AND trip_id<>?";
            PreparedStatement ps = conn.prepareStatement(blk);
            ps.setDate(1,start); ps.setDate(2,end);
            if (excludeTrip > 0) ps.setInt(3, excludeTrip);
            ResultSet rsBlk = ps.executeQuery();
            java.util.Set<Integer> blocked = new java.util.HashSet<>();
            while (rsBlk.next()) blocked.add(rsBlk.getInt("assignment_id"));
            for (int id : all) if (!blocked.contains(id)) available.add(id);
        } catch (Exception e) { e.printStackTrace(); }
        return available;
    }

    // ── View Trip Panel ───────────────────────────────────────────────────────
    public class ViewTripPanel extends VBox {
        private Label lId=vl(),lPax=vl(),lAdm=vl(),lAss=vl(),lSd=vl(),lSt=vl(),lEd=vl(),lEt=vl();
        private Label lPk=vl(),lDst=vl(),lPc=vl(),lStatus=vl();
        ViewTripPanel(){
            setBackground(Background.fill(Color.WHITE)); setAlignment(Pos.CENTER);
            VBox card=new VBox(0); card.getStyleClass().add("card"); card.setMaxWidth(560);
            Label title=new Label("Trip Details"); title.getStyleClass().add("title-medium");
            title.setStyle(title.getStyle()+"-fx-text-fill:#0096C7;");
            title.setMaxWidth(Double.MAX_VALUE); title.setAlignment(Pos.CENTER);
            GridPane grid=FxUtil.formGrid(); int y=0;
            FxUtil.addInfoRow(grid,"Trip ID:",     lId,    y++); FxUtil.addInfoRow(grid,"Passenger:", lPax,y++);
            FxUtil.addInfoRow(grid,"Admin:",       lAdm,   y++); FxUtil.addInfoRow(grid,"Assignment:",lAss,y++);
            FxUtil.addInfoRow(grid,"Start Date:",  lSd,    y++); FxUtil.addInfoRow(grid,"Start Time:",lSt,y++);
            FxUtil.addInfoRow(grid,"End Date:",    lEd,    y++); FxUtil.addInfoRow(grid,"End Time:",  lEt,y++);
            FxUtil.addInfoRow(grid,"Pickup:",      lPk,    y++); FxUtil.addInfoRow(grid,"Destination:",lDst,y++);
            FxUtil.addInfoRow(grid,"Pax Count:",   lPc,    y++); FxUtil.addInfoRow(grid,"Status:",   lStatus,y);
            Button btnBack=FxUtil.btnOutlinePrimary("Back");
            btnBack.setOnAction(e->{cards.show("LIST");loadTrips("All");});
            HBox btnRow=new HBox(btnBack); btnRow.setAlignment(Pos.CENTER); btnRow.setPadding(new Insets(20,0,0,0));
            card.getChildren().addAll(title,FxUtil.spacer(20),grid,btnRow);
            getChildren().add(card);
        }
        void load(int id){
            try{
                PreparedStatement ps=conn.prepareStatement(
                    "SELECT t.trip_id,u.first_name+' '+u.last_name AS passenger,ISNULL(au.first_name+' '+au.last_name,'—') AS admin_name," +
                    "ISNULL(du.first_name+' '+du.last_name+' - '+v.vehicle_model,'—') AS assignment," +
                    "t.start_date,t.start_time,t.end_date,t.end_time,t.pick_up_location,t.destination,t.passenger_count,t.trip_status " +
                    "FROM Trip t JOIN Passenger p ON t.passenger_id=p.passenger_id JOIN Users u ON p.passenger_id=u.user_id " +
                    "LEFT JOIN Admin a ON t.admin_id=a.admin_id LEFT JOIN Users au ON a.admin_id=au.user_id " +
                    "LEFT JOIN Vehicle_Assignment va ON t.assignment_id=va.assignment_id " +
                    "LEFT JOIN Driver d ON va.driver_id=d.driver_id LEFT JOIN Users du ON d.driver_id=du.user_id " +
                    "LEFT JOIN Vehicle v ON va.vehicle_id=v.vehicle_id WHERE t.trip_id=?");
                ps.setInt(1,id); ResultSet rs=ps.executeQuery();
                if(rs.next()){
                    lId.setText(String.valueOf(rs.getInt("trip_id")));
                    lPax.setText(rs.getString("passenger")); lAdm.setText(rs.getString("admin_name"));
                    lAss.setText(rs.getString("assignment"));
                    lSd.setText(rs.getDate("start_date")!=null?rs.getDate("start_date").toString():"—");
                    lSt.setText(rs.getTime("start_time")!=null?rs.getTime("start_time").toString():"—");
                    lEd.setText(rs.getDate("end_date")!=null?rs.getDate("end_date").toString():"—");
                    lEt.setText(rs.getTime("end_time")!=null?rs.getTime("end_time").toString():"—");
                    lPk.setText(rs.getString("pick_up_location")); lDst.setText(rs.getString("destination"));
                    lPc.setText(String.valueOf(rs.getInt("passenger_count")));
                    String s=rs.getString("trip_status"); lStatus.setText(s);
                    lStatus.setStyle(switch(s.toLowerCase()){
                        case "pending"   -> "-fx-text-fill:#E67E22;-fx-font-weight:bold;";
                        case "approved"  -> "-fx-text-fill:#0096C7;-fx-font-weight:bold;";
                        case "completed" -> "-fx-text-fill:#27AE60;-fx-font-weight:bold;";
                        case "cancelled" -> "-fx-text-fill:#DC3545;-fx-font-weight:bold;";
                        default          -> "-fx-font-weight:bold;";
                    });
                }
            }catch(Exception e){e.printStackTrace();}
        }
        //private Label vl(){Label l=new Label();l.getStyleClass().add("form-label");return l;}
    }

    // ── Create Trip Panel ─────────────────────────────────────────────────────
    public class CreateTripPanel extends VBox {
        private ComboBox<String> cmbPass=new ComboBox<>(), cmbAss=new ComboBox<>();
        private ComboBox<String> cmbStatus=FxUtil.styledCombo(FXCollections.observableArrayList("Pending","Approved","Completed","Cancelled"));
        private TextField txtSd=FxUtil.styledField(),txtSt=FxUtil.styledField(),txtEd=FxUtil.styledField(),txtEt=FxUtil.styledField();
        private TextField txtPk=FxUtil.styledField(),txtDst=FxUtil.styledField(),txtPax=FxUtil.styledField(),txtAdmin=FxUtil.readonlyField();
        private Label lblStatus=FxUtil.statusLabel();
        private int[] passengerIds=new int[0], assignmentIds=new int[0], assignCaps=new int[0];

        CreateTripPanel(){
            setBackground(Background.fill(Color.WHITE)); setAlignment(Pos.CENTER);
            cmbStatus.setValue("Pending");
            cmbPass.getStyleClass().add("combo-field"); cmbPass.setPrefWidth(260);
            cmbAss.getStyleClass().add("combo-field"); cmbAss.setPrefWidth(260);
            VBox card=new VBox(0); card.getStyleClass().add("card"); card.setMaxWidth(560);
            Label title=new Label("Create Trip"); title.getStyleClass().add("title-medium");
            title.setMaxWidth(Double.MAX_VALUE); title.setAlignment(Pos.CENTER);
            GridPane form=FxUtil.formGrid(); int y=0;
            FxUtil.addFormRow(form,"Passenger:",  cmbPass, y++); FxUtil.addFormRow(form,"Admin:",     txtAdmin,y++);
            FxUtil.addFormRow(form,"Start Date:", txtSd,   y++); FxUtil.addFormRow(form,"Start Time:",txtSt,  y++);
            FxUtil.addFormRow(form,"End Date:",   txtEd,   y++); FxUtil.addFormRow(form,"End Time:",  txtEt,  y++);
            FxUtil.addFormRow(form,"Assignment:", cmbAss,  y++); FxUtil.addFormRow(form,"Pickup:",    txtPk,  y++);
            FxUtil.addFormRow(form,"Destination:",txtDst,  y++); FxUtil.addFormRow(form,"Pax Count:", txtPax, y++);
            FxUtil.addFormRow(form,"Status:",     cmbStatus,y);
            Label hint=new Label("* Enter dates first, then click Check to see available assignments.");
            hint.setStyle("-fx-font-style:italic;-fx-font-size:11px;-fx-text-fill:#888;");
            Button btnCheck=FxUtil.btnOutlinePrimary("Check Available Assignments");
            btnCheck.setOnAction(e->checkAvailability());
            Button btnSave=FxUtil.btnPrimary("Save"), btnBack=FxUtil.btnOutlinePrimary("Back");
            btnSave.setOnAction(e->save()); btnBack.setOnAction(e->{cards.show("LIST");loadTrips("All");});
            HBox btnRow=new HBox(15,btnSave,btnBack); btnRow.setAlignment(Pos.CENTER); btnRow.setPadding(new Insets(15,0,0,0));
            card.getChildren().addAll(title,FxUtil.spacer(20),form,FxUtil.spacer(6),hint,FxUtil.spacer(8),btnCheck,FxUtil.spacer(14),btnRow,FxUtil.spacer(10),lblStatus);
            ScrollPane sp=new ScrollPane(card); sp.setFitToWidth(true); sp.getStyleClass().add("edge-to-edge");
            getChildren().add(sp);
        }
        public void setAdminId(int id){}
        void resetFields(){
            loadPassengers(); cmbAss.getItems().clear();
            assignmentIds=new int[0]; assignCaps=new int[0];
            txtSd.clear(); txtSt.setText("HH:MM"); txtEd.clear(); txtEt.setText("HH:MM");
            txtPk.clear(); txtDst.clear(); txtPax.clear(); cmbStatus.setValue("Pending");
            lblStatus.setText(" "); txtAdmin.setText(loggedInAdminName);
        }
        private void loadPassengers(){
            try{
                cmbPass.getItems().clear(); List<Integer> ids=new ArrayList<>();
                PreparedStatement ps=conn.prepareStatement("SELECT p.passenger_id, u.first_name+' '+u.last_name AS name FROM Passenger p JOIN Users u ON p.passenger_id=u.user_id WHERE u.user_status='Active'");
                ResultSet rs=ps.executeQuery();
                while(rs.next()){cmbPass.getItems().add(rs.getString("name"));ids.add(rs.getInt("passenger_id"));}
                passengerIds=ids.stream().mapToInt(i->i).toArray();
            }catch(Exception e){e.printStackTrace();}
        }
        private void checkAvailability(){
            java.sql.Date s,e2;
            try{s=java.sql.Date.valueOf(txtSd.getText().trim());}
            catch(Exception ex){FxUtil.setError(lblStatus,"Invalid start date! Use yyyy-MM-dd");return;}
            try{e2=java.sql.Date.valueOf(txtEd.getText().trim());}
            catch(Exception ex){FxUtil.setError(lblStatus,"Invalid end date! Use yyyy-MM-dd");return;}
            if(!s.toLocalDate().isAfter(LocalDate.now().minusDays(1))){FxUtil.setError(lblStatus,"Start date cannot be in the past!");return;}
            if(e2.before(s)){FxUtil.setError(lblStatus,"End date must be after start date!");return;}
            List<Integer> avail=availableAssignments(s,e2,-1);
            if(avail.isEmpty()){FxUtil.setError(lblStatus,"No available assignments for these dates!");cmbAss.getItems().clear();return;}
            try{
                cmbAss.getItems().clear(); List<Integer> aIds=new ArrayList<>(), caps=new ArrayList<>();
                for(int aId:avail){
                    PreparedStatement ps=conn.prepareStatement(
                        "SELECT va.assignment_id,du.first_name+' '+du.last_name+' - '+v.vehicle_model AS label,v.passenger_capacity " +
                        "FROM Vehicle_Assignment va JOIN Driver d ON va.driver_id=d.driver_id JOIN Users du ON d.driver_id=du.user_id " +
                        "JOIN Vehicle v ON va.vehicle_id=v.vehicle_id WHERE va.assignment_id=?");
                    ps.setInt(1,aId); ResultSet rs=ps.executeQuery();
                    if(rs.next()){cmbAss.getItems().add(rs.getString("label")+" (Cap: "+rs.getInt("passenger_capacity")+")");aIds.add(aId);caps.add(rs.getInt("passenger_capacity"));}
                }
                assignmentIds=aIds.stream().mapToInt(i->i).toArray();
                assignCaps=caps.stream().mapToInt(i->i).toArray();
                FxUtil.setSuccess(lblStatus,aIds.size()+" assignment(s) available.");
            }catch(Exception ex){ex.printStackTrace();}
        }
        private java.sql.Time parseTime(String s) throws Exception{if(s.matches("\\d{2}:\\d{2}"))s+=":00";return java.sql.Time.valueOf(s);}
        private void save(){
            if(loggedInAdminId<=0){FxUtil.setError(lblStatus,"No logged-in admin!");return;}
            if(passengerIds.length==0){FxUtil.setError(lblStatus,"No passengers available!");return;}
            if(assignmentIds.length==0){FxUtil.setError(lblStatus,"Run Check Available Assignments first!");return;}
            if(txtPk.getText().trim().isEmpty()){FxUtil.setError(lblStatus,"Pickup required!");return;}
            if(txtDst.getText().trim().isEmpty()){FxUtil.setError(lblStatus,"Destination required!");return;}
            java.sql.Date sd,ed; java.sql.Time st,et;
            try{sd=java.sql.Date.valueOf(txtSd.getText().trim());}catch(Exception ex){FxUtil.setError(lblStatus,"Invalid start date!");return;}
            try{ed=java.sql.Date.valueOf(txtEd.getText().trim());}catch(Exception ex){FxUtil.setError(lblStatus,"Invalid end date!");return;}
            if(!sd.toLocalDate().isAfter(LocalDate.now().minusDays(1))){FxUtil.setError(lblStatus,"Start date cannot be in the past!");return;}
            if(ed.before(sd)){FxUtil.setError(lblStatus,"End date must be after start date!");return;}
            try{st=parseTime(txtSt.getText().trim());}catch(Exception ex){FxUtil.setError(lblStatus,"Invalid start time! Use HH:MM");return;}
            try{et=parseTime(txtEt.getText().trim());}catch(Exception ex){FxUtil.setError(lblStatus,"Invalid end time! Use HH:MM");return;}
            int pax; try{pax=Integer.parseInt(txtPax.getText().trim());if(pax<=0)throw new Exception();}
            catch(Exception ex){FxUtil.setError(lblStatus,"Pax count must be a positive number!");return;}
            int selIdx=cmbAss.getSelectionModel().getSelectedIndex();
            if(selIdx<0){FxUtil.setError(lblStatus,"Select an assignment!");return;}
            int selAid=assignmentIds[selIdx];
            if(pax>assignCaps[selIdx]){FxUtil.setError(lblStatus,"Pax count exceeds vehicle capacity ("+assignCaps[selIdx]+")!");return;}
            if(!availableAssignments(sd,ed,-1).contains(selAid)){FxUtil.setError(lblStatus,"Assignment no longer available!");return;}
            try{
                PreparedStatement ps=conn.prepareStatement(
                    "INSERT INTO Trip (passenger_id,admin_id,assignment_id,start_date,start_time,end_date,end_time,pick_up_location,destination,passenger_count,trip_status) VALUES (?,?,?,?,?,?,?,?,?,?,?)");
                ps.setInt(1,passengerIds[cmbPass.getSelectionModel().getSelectedIndex()]); ps.setInt(2,loggedInAdminId);
                ps.setInt(3,selAid); ps.setDate(4,sd); ps.setTime(5,st); ps.setDate(6,ed); ps.setTime(7,et);
                ps.setString(8,txtPk.getText().trim()); ps.setString(9,txtDst.getText().trim());
                ps.setInt(10,pax); ps.setString(11,cmbStatus.getValue()); ps.executeUpdate();
                FxUtil.setSuccess(lblStatus,"Trip created successfully!"); loadTrips("All");
            }catch(Exception e){FxUtil.setError(lblStatus,"Error creating trip! "+e.getMessage());e.printStackTrace();}
        }
    }

    // ── Update Trip Panel ─────────────────────────────────────────────────────
    public class UpdateTripPanel extends VBox {
        private int tripId=-1;
        private ComboBox<String> cmbPass=new ComboBox<>(), cmbAss=new ComboBox<>();
        private ComboBox<String> cmbStatus=FxUtil.styledCombo(FXCollections.observableArrayList("Pending","Approved","Completed","Cancelled"));
        private TextField txtSd=FxUtil.styledField(),txtSt=FxUtil.styledField(),txtEd=FxUtil.styledField(),txtEt=FxUtil.styledField();
        private TextField txtPk=FxUtil.styledField(),txtDst=FxUtil.styledField(),txtPax=FxUtil.styledField(),txtAdmin=FxUtil.readonlyField();
        private Label lblStatus=FxUtil.statusLabel();
        private int[] passengerIds = new int[0];
        private int[] assignmentIds = new int[0];
        private int[] assignCaps = new int[0];
        private int curAssignId = -1;
        
        UpdateTripPanel(){
            setBackground(Background.fill(Color.WHITE)); setAlignment(Pos.CENTER);
            cmbPass.getStyleClass().add("combo-field"); cmbPass.setPrefWidth(260);
            cmbAss.getStyleClass().add("combo-field"); cmbAss.setPrefWidth(260);
            VBox card=new VBox(0); card.getStyleClass().add("card"); card.setMaxWidth(560);
            Label title=new Label("Update Trip"); title.getStyleClass().add("title-medium");
            title.setMaxWidth(Double.MAX_VALUE); title.setAlignment(Pos.CENTER);
            GridPane form=FxUtil.formGrid(); int y=0;
            FxUtil.addFormRow(form,"Passenger:",  cmbPass,   y++); FxUtil.addFormRow(form,"Admin:",     txtAdmin,  y++);
            FxUtil.addFormRow(form,"Assignment:", cmbAss,    y++); FxUtil.addFormRow(form,"Start Date:",txtSd,     y++);
            FxUtil.addFormRow(form,"Start Time:", txtSt,     y++); FxUtil.addFormRow(form,"End Date:",  txtEd,     y++);
            FxUtil.addFormRow(form,"End Time:",   txtEt,     y++); FxUtil.addFormRow(form,"Pickup:",    txtPk,     y++);
            FxUtil.addFormRow(form,"Destination:",txtDst,    y++); FxUtil.addFormRow(form,"Pax Count:", txtPax,    y++);
            FxUtil.addFormRow(form,"Status:",     cmbStatus, y);
            Button btnUpd=FxUtil.btnPrimary("Update"), btnBack=FxUtil.btnOutlinePrimary("Back");
            btnUpd.setOnAction(e->update()); btnBack.setOnAction(e->{cards.show("LIST");loadTrips("All");});
            HBox btnRow=new HBox(15,btnUpd,btnBack); btnRow.setAlignment(Pos.CENTER); btnRow.setPadding(new Insets(20,0,0,0));
            card.getChildren().addAll(title,FxUtil.spacer(20),form,btnRow,FxUtil.spacer(10),lblStatus);
            ScrollPane sp=new ScrollPane(card); sp.setFitToWidth(true); sp.getStyleClass().add("edge-to-edge");
            getChildren().add(sp);
        }
        void load(int id){
            tripId=id; lblStatus.setText(" "); loadDropdowns(); txtAdmin.setText(loggedInAdminName);
            try{
                PreparedStatement ps=conn.prepareStatement("SELECT * FROM Trip WHERE trip_id=?");
                ps.setInt(1,id); ResultSet rs=ps.executeQuery();
                if(rs.next()){
                	int pId = rs.getInt("passenger_id");

                	Integer aIdObj = (Integer) rs.getObject("assignment_id");
                	curAssignId = (aIdObj != null) ? aIdObj : -1;
                    
                	for(int i=0;i<passengerIds.length;i++) if(passengerIds[i]==pId){
                		cmbPass.getSelectionModel().select(i);
                		break;
                	}
                   
                    if(curAssignId!=-1) 
                        for(int i=0;i<assignmentIds.length;i++) 
                            if(assignmentIds[i]==curAssignId){
                                cmbAss.getSelectionModel().select(i);
                                break;
                            }                    
                    
                    txtSd.setText(rs.getDate("start_date")!=null?rs.getDate("start_date").toString():"");
                    txtSt.setText(rs.getTime("start_time")!=null?rs.getTime("start_time").toString():"HH:MM");
                    txtEd.setText(rs.getDate("end_date")!=null?rs.getDate("end_date").toString():"");
                    txtEt.setText(rs.getTime("end_time")!=null?rs.getTime("end_time").toString():"HH:MM");
                    txtPk.setText(rs.getString("pick_up_location")); txtDst.setText(rs.getString("destination"));
                    txtPax.setText(String.valueOf(rs.getInt("passenger_count"))); cmbStatus.setValue(rs.getString("trip_status"));
                }
            }catch(Exception e){e.printStackTrace();}
        }
        private void loadDropdowns(){
            try{
                cmbPass.getItems().clear(); List<Integer> pIds=new ArrayList<>();
                ResultSet rsP=conn.prepareStatement("SELECT p.passenger_id,u.first_name+' '+u.last_name AS name FROM Passenger p JOIN Users u ON p.passenger_id=u.user_id WHERE u.user_status='Active'").executeQuery();
                while(rsP.next()){cmbPass.getItems().add(rsP.getString("name"));pIds.add(rsP.getInt("passenger_id"));}
                passengerIds=pIds.stream().mapToInt(i->i).toArray();
                cmbAss.getItems().clear(); List<Integer> aIds=new ArrayList<>(), caps=new ArrayList<>();
                ResultSet rsA=conn.prepareStatement("SELECT va.assignment_id,du.first_name+' '+du.last_name+' - '+v.vehicle_model AS label,v.passenger_capacity FROM Vehicle_Assignment va JOIN Driver d ON va.driver_id=d.driver_id JOIN Users du ON d.driver_id=du.user_id JOIN Vehicle v ON va.vehicle_id=v.vehicle_id").executeQuery();
                while(rsA.next()){cmbAss.getItems().add(rsA.getString("label")+" (Cap: "+rsA.getInt("passenger_capacity")+")");aIds.add(rsA.getInt("assignment_id"));caps.add(rsA.getInt("passenger_capacity"));}
                assignmentIds=aIds.stream().mapToInt(i->i).toArray(); assignCaps=caps.stream().mapToInt(i->i).toArray();
            }catch(Exception e){e.printStackTrace();}
        }
        private java.sql.Time parseTime(String s) throws Exception{if(s.matches("\\d{2}:\\d{2}"))s+=":00";return java.sql.Time.valueOf(s);}
        private void update(){
            if(tripId==-1)return;
            if(loggedInAdminId<=0){FxUtil.setError(lblStatus,"No logged-in admin!");return;}
            if(txtPk.getText().trim().isEmpty()){FxUtil.setError(lblStatus,"Pickup required!");return;}
            if(txtDst.getText().trim().isEmpty()){FxUtil.setError(lblStatus,"Destination required!");return;}
            java.sql.Date sd,ed; java.sql.Time st,et;
            try{sd=java.sql.Date.valueOf(txtSd.getText().trim());}catch(Exception ex){FxUtil.setError(lblStatus,"Invalid start date!");return;}
            try{ed=java.sql.Date.valueOf(txtEd.getText().trim());}catch(Exception ex){FxUtil.setError(lblStatus,"Invalid end date!");return;}
            if(!sd.toLocalDate().isAfter(LocalDate.now().minusDays(1))){FxUtil.setError(lblStatus,"Start date cannot be in the past!");return;}
            if(ed.before(sd)){FxUtil.setError(lblStatus,"End date must be after start date!");return;}
            try{st=parseTime(txtSt.getText().trim());}catch(Exception ex){FxUtil.setError(lblStatus,"Invalid start time!");return;}
            try{et=parseTime(txtEt.getText().trim());}catch(Exception ex){FxUtil.setError(lblStatus,"Invalid end time!");return;}
            int pax; try{pax=Integer.parseInt(txtPax.getText().trim());if(pax<=0)throw new Exception();}
            catch(Exception ex){FxUtil.setError(lblStatus,"Pax count must be positive!");return;}
            int selIdx=cmbAss.getSelectionModel().getSelectedIndex();
            int selAid=(selIdx>=0&&assignmentIds.length>0)?assignmentIds[selIdx]:-1;
            if(selAid!=-1){
                if(pax>assignCaps[selIdx]){FxUtil.setError(lblStatus,"Pax count exceeds vehicle capacity ("+assignCaps[selIdx]+")!");return;}
                if(!availableAssignments(sd,ed,tripId).contains(selAid)){FxUtil.setError(lblStatus,"Assignment has overlapping trip!");return;}
            }
            try{
                PreparedStatement ps=conn.prepareStatement(
                    "UPDATE Trip SET passenger_id=?,admin_id=?,assignment_id=?,start_date=?,start_time=?,end_date=?,end_time=?,pick_up_location=?,destination=?,passenger_count=?,trip_status=? WHERE trip_id=?");
                ps.setInt(1,passengerIds[cmbPass.getSelectionModel().getSelectedIndex()]); ps.setInt(2,loggedInAdminId);
                if(selAid!=-1) ps.setInt(3,selAid); else ps.setNull(3,Types.INTEGER);
                ps.setDate(4,sd);ps.setTime(5,st);ps.setDate(6,ed);ps.setTime(7,et);
                ps.setString(8,txtPk.getText().trim());ps.setString(9,txtDst.getText().trim());
                ps.setInt(10,pax);ps.setString(11,cmbStatus.getValue());ps.setInt(12,tripId);
                ps.executeUpdate();
                FxUtil.setSuccess(lblStatus,"Trip updated successfully!"); loadTrips("All");
            }catch(Exception e){FxUtil.setError(lblStatus,"Error updating trip! "+e.getMessage());e.printStackTrace();}
        }
    }

    private Label vl(){Label l=new Label();l.getStyleClass().add("form-label");return l;}
}