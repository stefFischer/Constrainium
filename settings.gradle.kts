rootProject.name = "Constrainium"

include("core")
include("driver:rest")
findProject(":driver:rest")?.name = "rest"
include("test-systems:EMB:rest-ncs")
findProject(":test-systems:EMB:rest-ncs")?.name = "rest-ncs"
