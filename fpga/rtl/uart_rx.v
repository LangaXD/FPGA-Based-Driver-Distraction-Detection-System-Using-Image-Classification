`timescale 1ns / 1ps
//
// uart_rx - standard 8N1 UART receiver (8 data bits, no parity, 1 stop bit,
// LSB first). This is the piece that would sit on the physical FPGA board
// and receive classification bytes sent by a laptop running the trained CNN
// over a USB-serial cable.

module uart_rx #(
    parameter integer CLK_FREQ_HZ = 100_000_000,
    parameter integer BAUD_RATE   = 115200
)(
    input  wire       clk,
    input  wire       rst_n,
    input  wire       rx_serial,
    output reg  [7:0] rx_data,
    output reg        rx_valid   // pulses high for one clk cycle when rx_data is valid
);

    localparam integer CYCLES_PER_BIT = CLK_FREQ_HZ / BAUD_RATE;

    localparam [1:0] S_IDLE  = 2'd0;
    localparam [1:0] S_START = 2'd1;
    localparam [1:0] S_DATA  = 2'd2;
    localparam [1:0] S_STOP  = 2'd3;

    reg [1:0]  state;
    reg [15:0] clk_count;
    reg [2:0]  bit_index;
    reg [7:0]  rx_shift;

    // 2-flop synchroniser for the asynchronous serial input
    reg rx_meta, rx_sync;
    always @(posedge clk) begin
        rx_meta <= rx_serial;
        rx_sync <= rx_meta;
    end

    always @(posedge clk) begin
        if (!rst_n) begin
            state     <= S_IDLE;
            clk_count <= 16'd0;
            bit_index <= 3'd0;
            rx_data   <= 8'd0;
            rx_valid  <= 1'b0;
        end else begin
            rx_valid <= 1'b0;

            case (state)
                S_IDLE: begin
                    clk_count <= 16'd0;
                    bit_index <= 3'd0;
                    if (rx_sync == 1'b0)  // start bit detected
                        state <= S_START;
                end

                S_START: begin
                    // sample mid-bit to confirm this is a real start bit, not a glitch
                    if (clk_count == (CYCLES_PER_BIT / 2)) begin
                        if (rx_sync == 1'b0) begin
                            clk_count <= 16'd0;
                            state     <= S_DATA;
                        end else begin
                            state <= S_IDLE;
                        end
                    end else begin
                        clk_count <= clk_count + 1'b1;
                    end
                end

                S_DATA: begin
                    if (clk_count == CYCLES_PER_BIT - 1) begin
                        clk_count           <= 16'd0;
                        rx_shift[bit_index] <= rx_sync;
                        if (bit_index == 3'd7)
                            state <= S_STOP;
                        else
                            bit_index <= bit_index + 1'b1;
                    end else begin
                        clk_count <= clk_count + 1'b1;
                    end
                end

                S_STOP: begin
                    if (clk_count == CYCLES_PER_BIT - 1) begin
                        rx_data  <= rx_shift;
                        rx_valid <= 1'b1;
                        state    <= S_IDLE;
                    end else begin
                        clk_count <= clk_count + 1'b1;
                    end
                end

                default: state <= S_IDLE;
            endcase
        end
    end

endmodule
