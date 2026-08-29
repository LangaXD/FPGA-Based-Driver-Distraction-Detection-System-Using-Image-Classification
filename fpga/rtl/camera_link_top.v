`timescale 1ns / 1ps
//
// camera_link_top
//
// This is the module intended for the physical FPGA board: it receives
// classification results from a laptop running the trained CNN on a
// camera-connected webcam feed, sent over a UART link one byte at a time
// (byte 0 = class_id 0-9 in the low nibble, byte 1 = confidence 0-255),
// and drives the same distraction_alert_controller already verified
// standalone (see distraction_alert_controller.v / its testbench).
//
// In a full deployment, the laptop side would be the existing Python
// webcam inference loop (notebooks/09_cross_view_training_mobilenetv2.ipynb)
// with a serial.write() call added after each prediction. Nothing here
// runs the CNN itself - this is the receive + decide + alert stage that
// would sit on the FPGA.

module camera_link_top #(
    parameter integer CLK_FREQ_HZ = 100_000_000,
    parameter integer BAUD_RATE   = 115200
)(
    input  wire       clk,
    input  wire       rst_n,
    input  wire       uart_rx_serial,
    output wire        alert,
    output wire [3:0]  stable_class_id
);

    wire [7:0] rx_data;
    wire       rx_valid;

    uart_rx #(
        .CLK_FREQ_HZ(CLK_FREQ_HZ),
        .BAUD_RATE(BAUD_RATE)
    ) u_uart_rx (
        .clk(clk),
        .rst_n(rst_n),
        .rx_serial(uart_rx_serial),
        .rx_data(rx_data),
        .rx_valid(rx_valid)
    );

    reg       byte_phase;   // 0 = expecting the class_id byte, 1 = expecting the confidence byte
    reg [3:0] pending_class_id;
    reg       classification_valid;
    reg [7:0] classification_confidence;

    always @(posedge clk) begin
        if (!rst_n) begin
            byte_phase            <= 1'b0;
            pending_class_id      <= 4'd0;
            classification_valid  <= 1'b0;
        end else begin
            classification_valid <= 1'b0;

            if (rx_valid) begin
                if (byte_phase == 1'b0) begin
                    pending_class_id <= rx_data[3:0];
                    byte_phase       <= 1'b1;
                end else begin
                    classification_confidence <= rx_data;
                    classification_valid      <= 1'b1;
                    byte_phase                <= 1'b0;
                end
            end
        end
    end

    distraction_alert_controller u_alert_controller (
        .clk(clk),
        .rst_n(rst_n),
        .valid(classification_valid),
        .class_id(pending_class_id),
        .confidence(classification_confidence),
        .alert(alert),
        .stable_class_id(stable_class_id)
    );

endmodule
