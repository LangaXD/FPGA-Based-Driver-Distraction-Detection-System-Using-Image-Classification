`timescale 1ns / 1ps
//
// camera_link_top_tb
//
// End-to-end simulation of the full intended data path: a UART byte stream
// (as a laptop running the trained CNN would actually send) feeds
// camera_link_top, which decodes it and drives distraction_alert_controller.
//
// The 28-sample sequence below is NOT made up. It was produced by running
// the actual trained model (experiments/06_cross_view_mobilenetv2/
// best_mobilenetv2_crossview.keras) on 28 real test images, arranged as a
// realistic session: safe driving -> sustained real distraction (phone_left)
// -> back to safe driving. See fpga/reports/real_model_session_sequence.csv
// for the source data. It even includes two genuine model misclassifications
// (a phone_right blip at index 1, a texting_left blip at index 26) - both
// correctly absorbed by the debounce logic without triggering or re-triggering
// the alert, which is itself a meaningful result: real model noise, correctly
// handled by hardware.

module camera_link_top_tb;

    localparam integer CLK_FREQ_HZ = 100_000_000;
    localparam integer BAUD_RATE   = 115200;
    localparam integer CLK_PERIOD_NS = 10;                                  // 100 MHz
    localparam integer CYCLES_PER_BIT = CLK_FREQ_HZ / BAUD_RATE;            // matches uart_rx's own division
    localparam integer BIT_PERIOD_NS  = CYCLES_PER_BIT * CLK_PERIOD_NS;     // exact match to the receiver's bit timing

    reg clk;
    reg rst_n;
    reg uart_rx_serial;

    wire alert;
    wire [3:0] stable_class_id;

    integer errors;

    camera_link_top #(
        .CLK_FREQ_HZ(CLK_FREQ_HZ),
        .BAUD_RATE(BAUD_RATE)
    ) dut (
        .clk(clk),
        .rst_n(rst_n),
        .uart_rx_serial(uart_rx_serial),
        .alert(alert),
        .stable_class_id(stable_class_id)
    );

    initial clk = 1'b0;
    always #(CLK_PERIOD_NS/2) clk = ~clk;

    // Real model output sequence - see header comment above.
    localparam NUM_SAMPLES = 28;
    reg [3:0] sample_class_id  [0:NUM_SAMPLES-1];
    reg [7:0] sample_confidence[0:NUM_SAMPLES-1];

    initial begin
        sample_class_id[0]=4'd0;  sample_confidence[0]=8'd244;  // true: safe_driving
        sample_class_id[1]=4'd2;  sample_confidence[1]=8'd216;  // true: safe_driving (MODEL ERROR - real misclassification)
        sample_class_id[2]=4'd0;  sample_confidence[2]=8'd179;  // true: safe_driving
        sample_class_id[3]=4'd0;  sample_confidence[3]=8'd175;  // true: safe_driving
        sample_class_id[4]=4'd0;  sample_confidence[4]=8'd160;  // true: safe_driving
        sample_class_id[5]=4'd0;  sample_confidence[5]=8'd204;  // true: safe_driving
        sample_class_id[6]=4'd4;  sample_confidence[6]=8'd234;  // true: phone_left
        sample_class_id[7]=4'd4;  sample_confidence[7]=8'd250;  // true: phone_left
        sample_class_id[8]=4'd4;  sample_confidence[8]=8'd225;  // true: phone_left
        sample_class_id[9]=4'd4;  sample_confidence[9]=8'd185;  // true: phone_left
        sample_class_id[10]=4'd4; sample_confidence[10]=8'd242; // true: phone_left
        sample_class_id[11]=4'd4; sample_confidence[11]=8'd160; // true: phone_left
        sample_class_id[12]=4'd4; sample_confidence[12]=8'd181; // true: phone_left
        sample_class_id[13]=4'd4; sample_confidence[13]=8'd252; // true: phone_left
        sample_class_id[14]=4'd4; sample_confidence[14]=8'd232; // true: phone_left
        sample_class_id[15]=4'd4; sample_confidence[15]=8'd130; // true: phone_left
        sample_class_id[16]=4'd4; sample_confidence[16]=8'd189; // true: phone_left
        sample_class_id[17]=4'd4; sample_confidence[17]=8'd235; // true: phone_left
        sample_class_id[18]=4'd0; sample_confidence[18]=8'd249; // true: safe_driving
        sample_class_id[19]=4'd0; sample_confidence[19]=8'd221; // true: safe_driving
        sample_class_id[20]=4'd0; sample_confidence[20]=8'd224; // true: safe_driving
        sample_class_id[21]=4'd0; sample_confidence[21]=8'd251; // true: safe_driving
        sample_class_id[22]=4'd0; sample_confidence[22]=8'd229; // true: safe_driving
        sample_class_id[23]=4'd0; sample_confidence[23]=8'd152; // true: safe_driving
        sample_class_id[24]=4'd0; sample_confidence[24]=8'd219; // true: safe_driving
        sample_class_id[25]=4'd0; sample_confidence[25]=8'd128; // true: safe_driving
        sample_class_id[26]=4'd3; sample_confidence[26]=8'd170; // true: safe_driving (MODEL ERROR - real misclassification)
        sample_class_id[27]=4'd0; sample_confidence[27]=8'd250; // true: safe_driving
    end

    task uart_send_byte(input [7:0] data);
        integer i;
        begin
            uart_rx_serial = 1'b0;               // start bit
            #(BIT_PERIOD_NS);
            for (i = 0; i < 8; i = i + 1) begin
                uart_rx_serial = data[i];         // LSB first
                #(BIT_PERIOD_NS);
            end
            uart_rx_serial = 1'b1;                // stop bit
            #(BIT_PERIOD_NS);
        end
    endtask

    task uart_send_sample(input [3:0] cls, input [7:0] conf);
        begin
            uart_send_byte({4'd0, cls});
            uart_send_byte(conf);
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
        errors         = 0;
        rst_n          = 1'b0;
        uart_rx_serial = 1'b1;  // UART idle line is high

        repeat (5) @(posedge clk);
        rst_n = 1'b1;
        #(BIT_PERIOD_NS);

        // Samples 0-5: initial safe driving (includes one real misclassification at index 1)
        for (i = 0; i <= 5; i = i + 1)
            uart_send_sample(sample_class_id[i], sample_confidence[i]);
        check_alert(1'b0, "after real samples 0-5 (initial safe driving)");

        // Samples 6-17: sustained real distraction (phone_left)
        for (i = 6; i <= 17; i = i + 1)
            uart_send_sample(sample_class_id[i], sample_confidence[i]);
        check_alert(1'b1, "after real samples 6-17 (sustained distraction)");

        // Samples 18-24: returning to safe, not yet enough consecutive ticks to clear
        for (i = 18; i <= 24; i = i + 1)
            uart_send_sample(sample_class_id[i], sample_confidence[i]);
        check_alert(1'b1, "after real samples 18-24 (returning to safe, alert should still hold)");

        // Sample 25: the 8th consecutive confident-safe tick - alert should clear here
        uart_send_sample(sample_class_id[25], sample_confidence[25]);
        check_alert(1'b0, "after real sample 25 (alert should now clear)");

        // Sample 26: a real misclassification blip (texting_left) - must NOT re-trigger the alert
        uart_send_sample(sample_class_id[26], sample_confidence[26]);
        check_alert(1'b0, "after real sample 26 (single misclassification blip, should not re-trigger)");

        // Sample 27: final safe sample
        uart_send_sample(sample_class_id[27], sample_confidence[27]);
        check_alert(1'b0, "after real sample 27 (final state)");

        if (errors == 0)
            $display("\nTEST RESULT: ALL CHECKS PASSED (0 errors) - full real-data session correctly handled end to end");
        else
            $display("\nTEST RESULT: %0d CHECK(S) FAILED", errors);

        $finish;
    end

endmodule
