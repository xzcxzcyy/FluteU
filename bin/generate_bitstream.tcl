#Create output directory and clear contents
set outputdir [lindex $argv 0]
set sourcedir [lindex $argv 1]
set constrdir [lindex $argv 2]
file mkdir $outputdir
set files [glob -nocomplain "$outputdir/*"]
if {[llength $files] != 0} {
    puts "deleting contents of $outputdir"
    file delete -force {*}[glob -directory $outputdir *];
} else {
    puts "$outputdir is empty"
}

#Create project
create_project -part "xc7a100tcsg324-1" FluteU1 $outputdir

add_files -fileset sources_1 [glob $sourcedir/*.v]
add_files -fileset sources_1 [glob $sourcedir/*.mem]
add_files -fileset constrs_1 [glob $constrdir/*.xdc]

set_property top CPUTop [current_fileset]
update_compile_order -fileset sources_1

reset_run impl_1
reset_run synth_1
launch_runs -jobs 16 impl_1 -to_step write_bitstream
wait_on_run impl_1

exit
