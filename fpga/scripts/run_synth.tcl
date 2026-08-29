# Out-of-context synthesis of distraction_alert_controller, targeted at the
# Artix-7 part used on the Digilent Basys3 board (xc7a35tcpg236-1) - a
# widely used low-cost (~150 USD) academic FPGA board, matching this
# project's "low-cost solution" framing. Produces real utilization and
# timing reports, not estimates.

read_verilog -sv [glob [file dirname [info script]]/../rtl/*.v]

synth_design -top distraction_alert_controller -part xc7a35tcpg236-1 -mode out_of_context

# Define a 100 MHz clock so timing analysis is meaningful (out-of-context
# synthesis has no constraints by default).
create_clock -period 10.000 -name clk [get_ports clk]
set_property HD.CLK_SRC BUFGCTRL_X0Y0 [get_ports clk]

report_utilization -file [file dirname [info script]]/../reports/utilization.rpt
report_timing_summary -file [file dirname [info script]]/../reports/timing_summary.rpt
report_power -file [file dirname [info script]]/../reports/power.rpt

write_checkpoint -force [file dirname [info script]]/../reports/post_synth.dcp

puts "SYNTHESIS_SCRIPT_COMPLETE"
