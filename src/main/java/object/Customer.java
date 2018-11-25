package object;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.util.Duration;
import model.MainModel;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Customer extends StackPane {
    private Tooltip tooltip;
    private Arc arc;

    private static Image trolley;
    private int no;
    private int quantityOfGoods;
    private int quantityWaitForCheckout;
    private boolean isBeingServed;
    private boolean cannotWait;
    private int waitSec;
    private int waitSecActual;
    public Checkout parent;
    private ScheduledFuture<?> tooltipUpdateTask;
    private ScheduledFuture<?> timeCountTask;

    public Customer(int no, int quantityOfGoods, boolean cannotWait, int waitSec) {
        this.no = no;
        this.quantityOfGoods = quantityOfGoods;
        this.quantityWaitForCheckout = quantityOfGoods;
        this.cannotWait = cannotWait;
        this.waitSecActual = 0;
        this.waitSec = waitSec;
        this.isBeingServed = false;

        ImageView customerImageView = new ImageView();
        customerImageView.setFitHeight(150);
        customerImageView.setFitWidth(200);
        customerImageView.setPickOnBounds(true);
        customerImageView.setPreserveRatio(true);
        if (trolley == null) {
            trolley = new Image(getClass().getResourceAsStream("/image/trolley.png"));
        }
        customerImageView.setImage(trolley);

        tooltip = new Tooltip();
        tooltip.setShowDelay(Duration.ZERO);
        tooltip.setStyle("-fx-font-weight: bold");
        setOnMouseMoved(mouseEvent -> tooltip.show((Node) mouseEvent.getSource(),
                mouseEvent.getScreenX() + 5, mouseEvent.getScreenY() + 15));
        setOnMouseExited(mouseEvent -> tooltip.hide());
        initTooltipService(500);

        arc = new Arc(30, 30, 25, 25, 90, 0);
        arc.setFill(Paint.valueOf("#2197ff80"));
        arc.setStroke(Paint.valueOf("black"));
        arc.setStrokeType(StrokeType.INSIDE);
        arc.setType(ArcType.ROUND);
        Rectangle fill = new Rectangle(60, 60, Color.TRANSPARENT);
        Group group = new Group(arc, fill);
        StackPane.setAlignment(group, Pos.CENTER);

        getChildren().addAll(customerImageView, group);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), this);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
        initTimeCountService();
    }

    public void initTimeCountService() {
        if (timeCountTask != null) {
            timeCountTask.cancel(false);
        }
        if (isBeingServed) {
            return;
        }
        int playSpeedDivide = MainModel.getInstance().simulatorController.getPlaySpeedDivide();
        if (playSpeedDivide != 0) {
            int period = 1000000 / playSpeedDivide;
            timeCountTask = MainModel.getInstance().getThreadPoolExecutor().scheduleAtFixedRate(() -> {
                waitSecActual += 1;

                if (cannotWait && (waitSecActual >= waitSec)) {
                    MainModel.getInstance().leftCustomers.add(this);
                    MainModel.getInstance().outputController.customerLeaveEvent(this);
                    parent.getCustomers().remove(this);
                    //todo Platform.runLater(() -> ((Checkout) getParent()).getChildren().remove(this));
                    this.leave();
                } else {
                    initTimeCountService();
                }
            }, period, period, TimeUnit.MICROSECONDS);
        }
    }

    public void leave() {
        tooltipUpdateTask.cancel(false);
        timeCountTask.cancel(false);
    }

    public void initTooltipService(long period) {
        if (tooltipUpdateTask != null) {
            tooltipUpdateTask.cancel(false);
        }
        tooltipUpdateTask = MainModel.getInstance().getThreadPoolExecutor().scheduleAtFixedRate(() -> Platform.runLater(() -> {
            tooltip.setText("Customer " + no + "\n\n" +
                    "quantity of goods: " + quantityOfGoods + "\n" +
                    "waiting for served: " + quantityWaitForCheckout + "\n" +
                    "waiting time: " + waitSecActual + "s\n" +
                    "temper: " + (cannotWait ? "bad :(" : "good :)") +
                    (cannotWait ? "\nwill leave after:" + (waitSec - waitSecActual) + "s" : ""));
        }), 0, period, TimeUnit.MILLISECONDS);
    }

    public Arc getArc() {
        return arc;
    }

    public int getNo() {
        return no;
    }

    public int getQuantityOfGoods() {
        return quantityOfGoods;
    }

    public int getQuantityWaitForCheckout() {
        return quantityWaitForCheckout;
    }

    public void setQuantityWaitForCheckout(int quantityWaitForCheckout) {
        this.quantityWaitForCheckout = quantityWaitForCheckout;
    }

    public boolean isCannotWait() {
        return cannotWait;
    }

    public int getWaitSec() {
        return waitSec;
    }

    public boolean isBeingServed() {
        return isBeingServed;
    }

    public int getWaitSecActual() {
        return waitSecActual;
    }

    public void setBeingServed(boolean beingServed) {
        isBeingServed = beingServed;
    }
}
