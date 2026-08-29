# Physical pin assignment for the buzzer GPIO output.
# V7 = J62 pin 1 (PMOD2_0) - confirmed from the official ZC702 board user
# guide (UG850), Table 1-27 "GPIO Header Connections to XC7Z020 AP SoC at U1".
# LVCMOS33 because J62 is a 3.3V logic bank (confirmed from the same manual's
# User LED schematic, which shows these nets pulled to VCC3V3).
set_property PACKAGE_PIN V7 [get_ports {buzzer_o[0]}]
set_property IOSTANDARD LVCMOS33 [get_ports {buzzer_o[0]}]
