module annotation_tool.runner {
	exports annotation_tool.runner;
	opens annotation_tool.runner to javafx.fxml;
    requires javafx.controls;
    requires javafx.fxml;
	requires javafx.graphics;
	requires javafx.base;

}