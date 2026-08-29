# build_bitstream.tcl
# Stage 2: add the pin constraint, then run the real build - synthesis
# (turn the circuit into logic gates), implementation (decide where on the
# chip those gates physically go and how they're wired), and bitstream
# generation (the actual file that configures the FPGA fabric).

open_project buzzer_overlay.xpr

add_files -fileset constrs_1 -norecurse "pins.xdc"

puts ">>> STAGE2: launching synthesis..."
reset_run synth_1
launch_runs synth_1 -jobs 4
wait_on_run synth_1
if {[get_property PROGRESS [get_runs synth_1]] != "100%"} {
    error "Synthesis did not complete successfully"
}
puts ">>> STAGE2: synthesis done"

puts ">>> STAGE2: launching implementation..."
launch_runs impl_1 -jobs 4
wait_on_run impl_1
if {[get_property PROGRESS [get_runs impl_1]] != "100%"} {
    error "Implementation did not complete successfully"
}
puts ">>> STAGE2: implementation done"

puts ">>> STAGE2: generating bitstream..."
launch_runs impl_1 -to_step write_bitstream -jobs 4
wait_on_run impl_1
puts ">>> STAGE2: bitstream done"

# Hardware definition file for the software/PYNQ side - needs the
# implemented design open first.
open_run impl_1
file mkdir "export"
write_hwdef -force -file "export/buzzer_overlay.hwdef"

puts "STAGE2_OK: bitstream and hardware definition written"
