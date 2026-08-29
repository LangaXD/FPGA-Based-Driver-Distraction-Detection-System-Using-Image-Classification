# Creates a normal GUI-usable Vivado project wrapping the RTL + testbench
# already built and verified in batch mode (see fpga/README.md). Run once;
# after that, just open fpga/vivado_project/distraction_alert.xpr in Vivado.

set script_dir [file dirname [info script]]
set proj_dir   [file normalize "$script_dir/../vivado_project"]

create_project distraction_alert $proj_dir -part xc7a35tcpg236-1 -force

add_files -norecurse [list \
    [file normalize "$script_dir/../rtl/distraction_alert_controller.v"] \
    [file normalize "$script_dir/../rtl/uart_rx.v"] \
    [file normalize "$script_dir/../rtl/camera_link_top.v"] \
]
add_files -fileset sim_1 -norecurse [list \
    [file normalize "$script_dir/../sim/distraction_alert_controller_tb.v"] \
    [file normalize "$script_dir/../sim/camera_link_top_tb.v"] \
]

# camera_link_top_tb is the fuller end-to-end demo (real model data over a
# simulated UART link) - set as the default simulation top. Right-click
# distraction_alert_controller_tb in the Sources pane and choose "Set as Top"
# to switch back to the simpler standalone-controller testbench.
set_property top camera_link_top_tb [get_filesets sim_1]
set_property top camera_link_top [get_filesets sources_1]

update_compile_order -fileset sources_1
update_compile_order -fileset sim_1

puts "PROJECT_CREATED: $proj_dir/distraction_alert.xpr"
