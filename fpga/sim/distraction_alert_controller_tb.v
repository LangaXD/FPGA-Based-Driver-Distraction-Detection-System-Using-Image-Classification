`timescale 1ns / 1ps
//
// Self-checking testbench for distraction_alert_controller.
//
// Scenarios exercised (each checked against an expected alert value):
//   1. Sustained safe driving -> alert must never assert.
//   2. A SHORT burst of distraction (fewer ticks than ALERT_ON_COUNT) -> alert
//      must NOT assert. This is the key "no flicker on a single/few noisy
//      frame(s)" behaviour, the hardware mirror of the Python temporal
//      smoothing added to the webcam cell today.
//   3. SUSTAINED distraction (>= ALERT_ON_COUNT confident ticks) -> alert
//      must assert, and assert on exactly the expected tick.
//   4. A low-confidence ("uncertain") tick during a distracted run must not
//      reset the run counter.
//   5. Sustained safe driving after an alert -> alert must clear after
//      ALERT_OFF_COUNT confident-safe ticks.

module distraction_alert_controller_tb;

    localparam CLASS_WIDTH = 4;
    localparam CONF_WIDTH  = 8;
    localparam ALERT_ON_COUNT  = 8;
    localparam ALERT_OFF_COUNT = 8;

    reg clk;
    reg rst_n;
    reg valid;
    reg [CLASS_WIDTH-1:0] class_id;
    reg [CONF_WIDTH-1:0]  confidence;

    wire alert;
    wire [CLASS_WIDTH-1:0] stable_class_id;

    integer errors;

    distraction_alert_controller #(
        .CLASS_WIDTH(CLASS_WIDTH),
        .CONF_WIDTH(CONF_WIDTH),
        .ALERT_ON_COUNT(ALERT_ON_COUNT),
        .ALERT_OFF_COUNT(ALERT_OFF_COUNT)
    ) dut (
        .clk(clk),
        .rst_n(rst_n),
        .valid(valid),
        .class_id(class_id),
        .confidence(confidence),
        .alert(alert),
        .stable_class_id(stable_class_id)
    );

    // 100 MHz clock
    initial clk = 1'b0;
    always #5 clk = ~clk;

    task tick(input [CLASS_WIDTH-1:0] cls, input [CONF_WIDTH-1:0] conf, input do_valid);
        begin
            @(negedge clk);
            class_id   = cls;
            confidence = conf;
            valid      = do_valid;
            @(posedge clk);
            #1;
        end
    endtask

    task check_alert(input expected, input [255:0] label);
        begin
            if (alert !== expected) begin
                $display("FAIL [%0t] %0s : expected alert=%0d, got alert=%0d", $time, label, expected, alert);
                errors = errors + 1;
            end else begin
                $display("PASS [%0t] %0s : alert=%0d as expected", $time, label, alert);
            end
        end
    endtask

    integer i;

    initial begin
        errors     = 0;
        rst_n      = 1'b0;
        valid      = 1'b0;
        class_id   = {CLASS_WIDTH{1'b0}};
        confidence = {CONF_WIDTH{1'b0}};

        repeat (2) @(posedge clk);
        rst_n = 1'b1;
        check_alert(1'b0, "after reset");

        // 1. Sustained safe driving - alert must never assert
        for (i = 0; i < 10; i = i + 1)
            tick(4'd0, 8'd220, 1'b1); // class 0 = safe_driving, high confidence
        check_alert(1'b0, "after 10 confident safe ticks");

        // 2. Short distraction burst (3 ticks, below ALERT_ON_COUNT=8) - must NOT alert
        for (i = 0; i < 3; i = i + 1)
            tick(4'd4, 8'd200, 1'b1); // class 4 = phone_left
        check_alert(1'b0, "after 3 confident distracted ticks (transient, should be rejected)");

        // back to safe - counter should reset, not carry over
        tick(4'd0, 8'd220, 1'b1);
        check_alert(1'b0, "after returning to safe following a short burst");

        // 3. Sustained distraction: first ALERT_ON_COUNT-1 ticks must NOT yet alert
        for (i = 0; i < ALERT_ON_COUNT - 1; i = i + 1)
            tick(4'd2, 8'd230, 1'b1); // class 2 = phone_right
        check_alert(1'b0, "one tick before ALERT_ON_COUNT reached");

        // 4. A low-confidence ("uncertain") tick in the middle must not reset the run
        tick(4'd2, 8'd50, 1'b1); // confidence below CONF_THRESHOLD -> "uncertain"
        check_alert(1'b0, "uncertain tick should not itself trigger alert");

        // one more confident-distracted tick should now complete the run and trigger alert
        tick(4'd2, 8'd230, 1'b1);
        check_alert(1'b1, "alert should now be asserted after sustained distraction");

        // alert should remain asserted while distraction continues
        for (i = 0; i < 5; i = i + 1)
            tick(4'd2, 8'd230, 1'b1);
        check_alert(1'b1, "alert should remain asserted during continued distraction");

        // 5. Sustained safe driving should clear the alert after ALERT_OFF_COUNT ticks
        for (i = 0; i < ALERT_OFF_COUNT - 1; i = i + 1)
            tick(4'd0, 8'd220, 1'b1);
        check_alert(1'b1, "one tick before ALERT_OFF_COUNT reached, alert should still hold");

        tick(4'd0, 8'd220, 1'b1);
        check_alert(1'b0, "alert should now be cleared after sustained safe driving");

        if (errors == 0)
            $display("\nTEST RESULT: ALL CHECKS PASSED (0 errors)");
        else
            $display("\nTEST RESULT: %0d CHECK(S) FAILED", errors);

        $finish;
    end

endmodule
