package com.admin.panel;

import com.project.dbConnection.DbConnectMsSql;
import com.project.util.CardPane;
import com.project.util.FxUtil;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class VehiclePanel extends BorderPane {

    private final CardPane cards = new CardPane();
    private ObservableList<Object[]> tableData;
    private TableView<Object[]> table;
    private ComboBox<String> cmbFilter;
    private Connection conn;

    private UpdateVehiclePanel updatePanel;
    private MakeUnavailablePanel unavailablePanel;

    public VehiclePanel() {
        DbConnectMsSql db = new DbConnectMsSql();
        conn = db.conn;
        setBackground(Background.fill(Color.WHITE));

        updatePanel      = new UpdateVehiclePanel();
        unavailablePanel = new MakeUnavailablePanel();

        cards.addCard("LIST",            buildListPanel());
        cards.addCard("ADD",             new AddVehiclePanel());
        cards.addCard("UPDATE",          updatePanel);
        cards.addCard("MAKE_UNAVAILABLE", unavailablePanel);
        cards.show("LIST");
        setCenter(cards);
        loadVehicles("All");
    }

    // ─────────────────────────────────────────────────────────────────────────
    private BorderPane buildListPanel() {
        BorderPane main = new BorderPane();
        main.setBackground(Background.fill(Color.WHITE));
        main.setPadding(new Insets(20, 30, 20, 30));

        HBox topBar = new HBox(8);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 10, 0));

        cmbFilter = FxUtil.styledCombo(javafx.collections.FXCollections.observableArrayList(
            "All","Available","Not Available"));
        cmbFilter.setValue("All");
        cmbFilter.setPrefWidth(140);
        cmbFilter.setOnAction(e -> loadVehicles(cmbFilter.getValue()));

        Button btnRefresh = FxUtil.btnPrimary("Refresh");
        btnRefresh.setOnAction(e -> { cmbFilter.setValue("All"); loadVehicles("All"); });

        topBar.getChildren().addAll(new Label("Filter by Status:"), cmbFilter, FxUtil.hgrow(), btnRefresh);

        table = FxUtil.buildTable("Vehicle ID","Model","Plate No.","Type","Capacity","Status");
        FxUtil.applyStatusRenderer(table, 5);
        tableData = FxUtil.tableData(table);

        HBox bottom = new HBox(8);
        bottom.setAlignment(Pos.CENTER_RIGHT);
        bottom.setPadding(new Insets(8, 0, 0, 0));

        Button btnAdd  = FxUtil.btnPrimary("Add");
        Button btnUpd  = FxUtil.btnPrimary("Update");
        Button btnUna  = FxUtil.btnDanger("Make Unavailable");

        btnAdd.setOnAction(e -> cards.show("ADD"));
        btnUpd.setOnAction(e -> {
            Object[] row = table.getSelectionModel().getSelectedItem();
            if (row == null) { FxUtil.showInfo(this, "Select a vehicle first!"); return; }
            int id = (int) row[0];
            try {
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM Vehicle WHERE vehicle_id=?");
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    updatePanel.setData(rs.getInt("vehicle_id"), rs.getString("vehicle_model"),
                        rs.getString("plate_number"), rs.getString("vehicle_type"),
                        rs.getInt("passenger_capacity"), rs.getString("vehicle_status"));
                }
            } catch (Exception ex) { ex.printStackTrace(); }
            cards.show("UPDATE");
        });
        btnUna.setOnAction(e -> {
            Object[] row = table.getSelectionModel().getSelectedItem();
            if (row == null) { FxUtil.showInfo(this, "Select a vehicle first!"); return; }
            unavailablePanel.setDetails((int)row[0],(String)row[1],(String)row[2],
                (String)row[3],(int)row[4],(String)row[5]);
            cards.show("MAKE_UNAVAILABLE");
        });
        bottom.getChildren().addAll(btnAdd, btnUpd, btnUna);

        main.setTop(topBar);
        main.setCenter(FxUtil.tableScroll(table));
        main.setBottom(bottom);
        return main;
    }

    private void loadVehicles(String filter) {
        try {
            tableData.clear();
            String sql = "All".equals(filter)
                ? "SELECT vehicle_id,vehicle_model,plate_number,vehicle_type,passenger_capacity,vehicle_status FROM Vehicle"
                : "SELECT vehicle_id,vehicle_model,plate_number,vehicle_type,passenger_capacity,vehicle_status FROM Vehicle WHERE vehicle_status=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            if (!"All".equals(filter)) ps.setString(1, filter);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tableData.add(new Object[]{
                    rs.getInt("vehicle_id"), rs.getString("vehicle_model"),
                    rs.getString("plate_number"), rs.getString("vehicle_type"),
                    rs.getInt("passenger_capacity"), rs.getString("vehicle_status")
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void cascadeVehicleUnavailable(int vehicleId) throws Exception {
        PreparedStatement psFind = conn.prepareStatement(
            "SELECT assignment_id FROM Vehicle_Assignment WHERE vehicle_id=? AND assignment_status='Active'");
        psFind.setInt(1, vehicleId);
        ResultSet rs = psFind.executeQuery();
        List<Integer> ids = new ArrayList<>();
        while (rs.next()) ids.add(rs.getInt("assignment_id"));
        for (int aId : ids) {
            conn.prepareStatement("UPDATE Vehicle_Assignment SET assignment_status='Inactive' WHERE assignment_id=" + aId).executeUpdate();
            conn.prepareStatement("UPDATE Trip SET trip_status='Pending' WHERE assignment_id=" + aId + " AND trip_status='Approved'").executeUpdate();
        }
    }

    // ── Add Vehicle ───────────────────────────────────────────────────────────
    class AddVehiclePanel extends VBox {
        private final TextField txtModel    = FxUtil.styledField();
        private final TextField txtPlate    = FxUtil.styledField();
        private final TextField txtType     = FxUtil.styledField();
        private final TextField txtCapacity = FxUtil.styledField();
        private final ComboBox<String> cmbStatus = FxUtil.styledCombo(
            javafx.collections.FXCollections.observableArrayList("Available","Not Available"));
        private final Label lblStatus = FxUtil.statusLabel();

        AddVehiclePanel() {
        	lblStatus.setMaxWidth(Double.MAX_VALUE);
        	lblStatus.setAlignment(Pos.CENTER);
            setBackground(Background.fill(Color.WHITE));
            setAlignment(Pos.CENTER);

            cmbStatus.setValue("Available");
            VBox card = new VBox(0);
            card.getStyleClass().add("card");
            card.setMaxWidth(520);

            Label title = new Label("Add Vehicle");
            title.getStyleClass().add("title-medium");
            title.setMaxWidth(Double.MAX_VALUE);
            title.setAlignment(Pos.CENTER);

            GridPane form = FxUtil.formGrid();
            int y = 0;
            FxUtil.addFormRow(form, "Model:",     txtModel,    y++);
            FxUtil.addFormRow(form, "Plate No.:", txtPlate,    y++);
            FxUtil.addFormRow(form, "Type:",      txtType,     y++);
            FxUtil.addFormRow(form, "Capacity:",  txtCapacity, y++);
            FxUtil.addFormRow(form, "Status:",    cmbStatus,   y);

            Button btnAdd  = FxUtil.btnPrimary("Add");
            Button btnBack = FxUtil.btnOutlinePrimary("Back");
            btnAdd.setOnAction(e -> save());
            btnBack.setOnAction(e -> { clear(); cards.show("LIST"); loadVehicles("All"); });

            HBox btnRow = new HBox(15, btnAdd, btnBack);
            btnRow.setAlignment(Pos.CENTER);
            btnRow.setPadding(new Insets(20, 0, 0, 0));

            card.getChildren().addAll(title, FxUtil.spacer(20), form, btnRow, FxUtil.spacer(10), lblStatus);
            getChildren().add(card);
        }

        private void save() {
            String model = txtModel.getText().trim(), plate = txtPlate.getText().trim();
            String type  = txtType.getText().trim(),  cap   = txtCapacity.getText().trim();
            if (model.isEmpty()||plate.isEmpty()||type.isEmpty()||cap.isEmpty()) {
                FxUtil.setError(lblStatus,"All fields required!"); return; }
            int capacity;
            try { capacity = Integer.parseInt(cap); if (capacity<=0) throw new Exception(); }
            catch (Exception e) { FxUtil.setError(lblStatus,"Capacity must be a positive number!"); return; }
            try {
                PreparedStatement chk = conn.prepareStatement("SELECT COUNT(*) FROM Vehicle WHERE plate_number=?");
                chk.setString(1, plate);
                ResultSet rc = chk.executeQuery();
                if (rc.next() && rc.getInt(1) > 0) { FxUtil.setError(lblStatus,"Plate number already exists!"); return; }
                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Vehicle (vehicle_model,plate_number,vehicle_type,passenger_capacity,vehicle_status) VALUES (?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
                ps.setString(1,model); ps.setString(2,plate); ps.setString(3,type);
                ps.setInt(4,capacity); ps.setString(5,cmbStatus.getValue());
                ps.executeUpdate();
                FxUtil.setSuccess(lblStatus,"Vehicle added successfully!");
                clear(); loadVehicles("All");
            } catch (Exception e) { FxUtil.setError(lblStatus,"Error adding vehicle!"); e.printStackTrace(); }
        }
        private void clear() { txtModel.clear();txtPlate.clear();txtType.clear();txtCapacity.clear(); }
    }

    // ── Update Vehicle ────────────────────────────────────────────────────────
    class UpdateVehiclePanel extends VBox {
        private int vehicleId; private String prevStatus="", origPlate="";
        private final TextField txtModel    = FxUtil.styledField();
        private final TextField txtPlate    = FxUtil.styledField();
        private final TextField txtType     = FxUtil.styledField();
        private final TextField txtCapacity = FxUtil.styledField();
        private final ComboBox<String> cmbStatus = FxUtil.styledCombo(
            javafx.collections.FXCollections.observableArrayList("Available","Not Available"));
        private final Label lblStatus = FxUtil.statusLabel();

        UpdateVehiclePanel() {
        	lblStatus.setMaxWidth(Double.MAX_VALUE);
        	lblStatus.setAlignment(Pos.CENTER);
            setBackground(Background.fill(Color.WHITE));
            setAlignment(Pos.CENTER);
            cmbStatus.setValue("Available");
            VBox card = new VBox(0);
            card.getStyleClass().add("card");
            card.setMaxWidth(520);
            Label title = new Label("Update Vehicle");
            title.getStyleClass().add("title-medium");
            title.setMaxWidth(Double.MAX_VALUE); title.setAlignment(Pos.CENTER);
            GridPane form = FxUtil.formGrid();
            int y=0;
            FxUtil.addFormRow(form,"Model:",    txtModel,   y++);
            FxUtil.addFormRow(form,"Plate No.:",txtPlate,   y++);
            FxUtil.addFormRow(form,"Type:",     txtType,    y++);
            FxUtil.addFormRow(form,"Capacity:", txtCapacity,y++);
            FxUtil.addFormRow(form,"Status:",   cmbStatus,  y);
            Button btnUpd = FxUtil.btnPrimary("Update");
            Button btnBack= FxUtil.btnOutlinePrimary("Back");
            btnUpd.setOnAction(e->update());
            btnBack.setOnAction(e->{lblStatus.setText(" ");cards.show("LIST");loadVehicles("All");});
            HBox btnRow = new HBox(15,btnUpd,btnBack);
            btnRow.setAlignment(Pos.CENTER); btnRow.setPadding(new Insets(20,0,0,0));
            card.getChildren().addAll(title,FxUtil.spacer(20),form,btnRow,FxUtil.spacer(10),lblStatus);
            getChildren().add(card);
        }
        void setData(int id,String model,String plate,String type,int cap,String status){
            vehicleId=id; prevStatus=status!=null?status:"Available"; origPlate=plate!=null?plate:"";
            txtModel.setText(model); txtPlate.setText(plate); txtType.setText(type);
            txtCapacity.setText(String.valueOf(cap)); cmbStatus.setValue(status); lblStatus.setText(" ");
        }
        private void update(){
            String model=txtModel.getText().trim(),plate=txtPlate.getText().trim();
            String type=txtType.getText().trim(),cap=txtCapacity.getText().trim();
            String newStatus=cmbStatus.getValue();
            if(model.isEmpty()||plate.isEmpty()||type.isEmpty()||cap.isEmpty()){
                FxUtil.setError(lblStatus,"All fields required!"); return;}
            int capacity;
            try{capacity=Integer.parseInt(cap);if(capacity<=0)throw new Exception();}
            catch(Exception e){FxUtil.setError(lblStatus,"Capacity must be positive!");return;}
            try{
                PreparedStatement chk=conn.prepareStatement(
                    "SELECT COUNT(*) FROM Vehicle WHERE plate_number=? AND vehicle_id<>?");
                chk.setString(1,plate);chk.setInt(2,vehicleId);
                ResultSet rc=chk.executeQuery();
                if(rc.next()&&rc.getInt(1)>0){FxUtil.setError(lblStatus,"Plate already exists!");return;}
                PreparedStatement ps=conn.prepareStatement(
                    "UPDATE Vehicle SET vehicle_model=?,plate_number=?,vehicle_type=?,passenger_capacity=?,vehicle_status=? WHERE vehicle_id=?");
                ps.setString(1,model);ps.setString(2,plate);ps.setString(3,type);
                ps.setInt(4,capacity);ps.setString(5,newStatus);ps.setInt(6,vehicleId);
                ps.executeUpdate();
                if("Available".equalsIgnoreCase(prevStatus)&&"Not Available".equalsIgnoreCase(newStatus))
                    cascadeVehicleUnavailable(vehicleId);
                prevStatus=newStatus; origPlate=plate;
                FxUtil.setSuccess(lblStatus,"Vehicle updated successfully!");
                loadVehicles("All");
            }catch(Exception e){FxUtil.setError(lblStatus,"Error updating vehicle!");e.printStackTrace();}
        }
    }

    // ── Make Unavailable ──────────────────────────────────────────────────────
    class MakeUnavailablePanel extends VBox {
        private int vid;
        private final Label lblVid=valueLbl(),lblModel=valueLbl(),lblPlate=valueLbl();
        private final Label lblType=valueLbl(),lblCap=valueLbl(),lblStat=valueLbl();
        private final Label lblMsg = FxUtil.statusLabel();

        MakeUnavailablePanel(){
        	lblMsg.setMaxWidth(Double.MAX_VALUE);
        	lblMsg.setAlignment(Pos.CENTER);
            setBackground(Background.fill(Color.WHITE)); setAlignment(Pos.CENTER);
            VBox card=new VBox(0); card.getStyleClass().add("card"); card.setMaxWidth(520);
            Label title=new Label("Make Vehicle Unavailable");
            title.setStyle("-fx-font-weight:bold;-fx-font-size:20px;-fx-text-fill:#DC3545;");
            title.setMaxWidth(Double.MAX_VALUE); title.setAlignment(Pos.CENTER);
            GridPane grid=FxUtil.formGrid(); int y=0;
            FxUtil.addInfoRow(grid,"Vehicle ID:",lblVid,   y++);
            FxUtil.addInfoRow(grid,"Model:",     lblModel, y++);
            FxUtil.addInfoRow(grid,"Plate No.:", lblPlate, y++);
            FxUtil.addInfoRow(grid,"Type:",      lblType,  y++);
            FxUtil.addInfoRow(grid,"Capacity:",  lblCap,   y++);
            FxUtil.addInfoRow(grid,"Status:",    lblStat,  y);
            Label warn=new Label("This will also set related active assignments to Inactive\nand revert any Approved trips back to Pending.");
            warn.setStyle("-fx-font-style:italic;-fx-font-size:11px;-fx-text-fill:#B46400;");
            warn.setPadding(new Insets(8,0,8,0));
            Button btnConfirm=FxUtil.btnDanger("Confirm");
            Button btnBack=FxUtil.btnOutlinePrimary("Back");
            btnConfirm.setOnAction(e->makeUnavailable());
            btnBack.setOnAction(e->{cards.show("LIST");loadVehicles("All");});
            HBox btnRow=new HBox(15,btnConfirm,btnBack);
            btnRow.setAlignment(Pos.CENTER); btnRow.setPadding(new Insets(16,0,0,0));
            card.getChildren().addAll(title,FxUtil.spacer(20),grid,warn,btnRow,FxUtil.spacer(10),lblMsg);
            getChildren().add(card);
        }
        void setDetails(int id,String model,String plate,String type,int cap,String status){
            vid=id; lblVid.setText(String.valueOf(id)); lblModel.setText(model);
            lblPlate.setText(plate); lblType.setText(type); lblCap.setText(String.valueOf(cap));
            lblStat.setText(status); lblMsg.setText(" ");
        }
        private void makeUnavailable(){
            if(!FxUtil.confirm(this,"Set this vehicle to 'Not Available'?\nActive assignments and Approved trips will be affected.","Confirm"))return;
            try{
                conn.prepareStatement("UPDATE Vehicle SET vehicle_status='Not Available' WHERE vehicle_id="+vid).executeUpdate();
                cascadeVehicleUnavailable(vid);
                loadVehicles("All"); cards.show("LIST");
            }catch(Exception e){FxUtil.setError(lblMsg,"Error updating vehicle!");e.printStackTrace();}
        }
        private Label valueLbl(){Label l=new Label();l.getStyleClass().add("form-label");return l;}
    }
}