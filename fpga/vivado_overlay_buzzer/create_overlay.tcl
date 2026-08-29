# create_overlay.tcl
#
# Builds a minimal PYNQ-loadable overlay for the ZC702 board: just enough
# circuitry to let Python (running on the board's Linux) turn one GPIO
# fabric pin (J62 pin 1 / PMOD2_0 / package pin V7) on and off, so it can
# drive a buzzer through a transistor.
#
# This is a "block design" - Vivado's drag-and-drop circuit canvas - built
# here by script instead of by hand, so it's reproducible.

set proj_name "buzzer_overlay"
set proj_dir  [file normalize "."]
set part      "xc7z020clg484-1"

create_project $proj_name $proj_dir -part $part -force

# The block design canvas.
create_bd_design "buzzer_bd"

# --- Block 1: the Zynq Processing System (the ARM/Linux side of the chip) ---
# This IP block represents the already-running processor. We're not
# reconfiguring the processor itself - Linux is already booted from the SD
# card independently of this design. We add this block purely so Vivado can
# generate the AXI bus plumbing (clock, reset, address decoding) that lets
# the processor talk to whatever we build next.
create_bd_cell -type ip -vlnv xilinx.com:ip:processing_system7:5.5 ps7_0
apply_bd_automation -rule xilinx.com:bd_rule:processing_system7 \
    -config {make_external "FIXED_IO, DDR" apply_board_preset "0" \
              Master "Disable" Slave "Disable"} \
    [get_bd_cells ps7_0]

# Only the PL (fabric) side of the processor needs a clock for our tiny
# design - enable FCLK_CLK0 (a general-purpose 100 MHz clock the PS
# provides to the fabric) and enable the AXI general-purpose master port,
# which is how the PS talks out to fabric peripherals like our GPIO block.
set_property -dict [list \
    CONFIG.PCW_USE_FABRIC_INTERRUPT {0} \
    CONFIG.PCW_EN_CLK0_PORT {1} \
    CONFIG.PCW_FPGA0_PERIPHERAL_FREQMHZ {100} \
    CONFIG.PCW_USE_M_AXI_GP0 {1} \
] [get_bd_cells ps7_0]

# --- Block 2: AXI GPIO - the actual "controllable switch" ---
# One output bit, all-outputs (we only need to drive out, never read in).
create_bd_cell -type ip -vlnv xilinx.com:ip:axi_gpio:2.0 buzzer_gpio
set_property -dict [list \
    CONFIG.C_GPIO_WIDTH {1} \
    CONFIG.C_ALL_OUTPUTS {1} \
] [get_bd_cells buzzer_gpio]

# --- Wire them together ---
# This is the equivalent of Vivado's "Run Connection Automation": it adds
# the AXI interconnect, clock, and reset logic needed to connect the PS's
# AXI master port to the GPIO block's AXI slave port.
apply_bd_automation -rule xilinx.com:bd_rule:axi4 \
    -config { Master "/ps7_0/M_AXI_GP0" Clk "Auto" } \
    [get_bd_intf_pins buzzer_gpio/S_AXI]
# Note: the axi4 automation above already auto-connects the clock/reset
# nets (its "Clk Auto" setting), so a separate clkrst automation step here
# would find nothing left to connect and error out - confirmed by actually
# running this and seeing exactly that.

# --- Bring the GPIO's output wire out to a real top-level pin ---
# This Vivado version's make_bd_pins_external doesn't take a -name option
# (confirmed via its own -help output) - it auto-names the new port from
# the pin name. Rename it afterwards for clarity.
make_bd_pins_external [get_bd_pins buzzer_gpio/gpio_io_o]
set_property name buzzer_o [get_bd_ports gpio_io_o_0]

regenerate_bd_layout
validate_bd_design
save_bd_design

# Generate the actual HDL wrapper file Vivado will synthesise.
make_wrapper -files [get_files buzzer_bd.bd] -top
add_files -norecurse \
    [file join $proj_dir "$proj_name.srcs" "sources_1" "bd" "buzzer_bd" "hdl" "buzzer_bd_wrapper.v"]
set_property top buzzer_bd_wrapper [current_fileset]
update_compile_order -fileset sources_1

puts "STAGE1_OK: block design created and validated"
