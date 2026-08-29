`timescale 1ns / 1ps
//
// distraction_alert_controller
//
// Purpose: takes the CNN's per-frame prediction (class_id, confidence) and
// raises a sustained ALERT output only after several consecutive confident
// "distracted" classifications in a row - never on a single frame.
//
// This is the fixed-point hardware counterpart of the temporal-smoothing
// logic already used on the software side (notebooks/09_cross_view_training_
// mobilenetv2.ipynb, webcam cell): a single frame's prediction is noisy, so
// neither the Python display nor this hardware alert should react to it
// alone. CONF_THRESHOLD below (102/255 ~= 0.40) matches the Python
// CONFIDENCE_THRESHOLD = 0.40 used there, for the same reason.
//
// This module does not run the CNN itself - it is the decision/alert stage
// that would sit downstream of a quantised classifier on FPGA. class_id and
// confidence are external inputs, driven by a testbench here and, in a full
// system, by the classifier's output register.

module distraction_alert_controller #(
    parameter integer CLASS_WIDTH     = 4,   // 0-9 driver-behaviour classes fit in 4 bits
    parameter integer CONF_WIDTH      = 8,   // confidence as Q0.8 fixed-point, i.e. 0-255 maps to 0.0-1.0
    parameter integer CNT_WIDTH       = 4,   // counter width - must hold ALERT_ON_COUNT/ALERT_OFF_COUNT
    parameter [CLASS_WIDTH-1:0] SAFE_CLASS     = 4'd0,   // class 0 = safe_driving
    parameter [CONF_WIDTH-1:0]  CONF_THRESHOLD = 8'd102, // ~0.40 in Q0.8 - matches the Python confidence gate
    parameter integer ALERT_ON_COUNT  = 8,   // consecutive confident-distracted ticks needed to RAISE the alert
    parameter integer ALERT_OFF_COUNT = 8    // consecutive confident-safe ticks needed to CLEAR the alert
)(
    input  wire                     clk,
    input  wire                     rst_n,            // active-low synchronous reset
    input  wire                     valid,             // pulses for one cycle when a new classification is ready
    input  wire [CLASS_WIDTH-1:0]   class_id,          // 0-9, the CNN's argmax output for this tick
    input  wire [CONF_WIDTH-1:0]    confidence,        // 0-255, the CNN's softmax confidence for class_id
    output reg                      alert,             // sustained-distraction alert - drives an LED/buzzer
    output reg  [CLASS_WIDTH-1:0]   stable_class_id    // most recent classification, for display/logging
);

    reg [CNT_WIDTH-1:0] distracted_run;
    reg [CNT_WIDTH-1:0] safe_run;

    wire is_distracted = valid && (class_id != SAFE_CLASS) && (confidence >= CONF_THRESHOLD);
    wire is_safe       = valid && (class_id == SAFE_CLASS) && (confidence >= CONF_THRESHOLD);
    // Low-confidence ticks (neither is_distracted nor is_safe) are treated as "uncertain":
    // they hold both run-counters steady rather than resetting them, mirroring the Python
    // webcam cell's "uncertain" display state.

    always @(posedge clk) begin
        if (!rst_n) begin
            alert           <= 1'b0;
            stable_class_id <= SAFE_CLASS;
            distracted_run  <= {CNT_WIDTH{1'b0}};
            safe_run        <= {CNT_WIDTH{1'b0}};
        end else begin
            if (valid)
                stable_class_id <= class_id;

            if (is_distracted) begin
                if (distracted_run < ALERT_ON_COUNT[CNT_WIDTH-1:0])
                    distracted_run <= distracted_run + 1'b1;
                safe_run <= {CNT_WIDTH{1'b0}};
            end else if (is_safe) begin
                if (safe_run < ALERT_OFF_COUNT[CNT_WIDTH-1:0])
                    safe_run <= safe_run + 1'b1;
                distracted_run <= {CNT_WIDTH{1'b0}};
            end

            if (!alert && is_distracted && (distracted_run >= (ALERT_ON_COUNT[CNT_WIDTH-1:0] - 1'b1)))
                alert <= 1'b1;
            else if (alert && is_safe && (safe_run >= (ALERT_OFF_COUNT[CNT_WIDTH-1:0] - 1'b1)))
                alert <= 1'b0;
        end
    end

endmodule
