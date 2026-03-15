rootProject.name = "Constrainium"

include("core")

include("generator:random")
findProject(":generator:random")?.name = "random"


include("driver:rest")
findProject(":driver:rest")?.name = "rest"


include("test-systems:TestSystemRunner")
findProject(":test-systems:TestSystemRunner")?.name = "TestSystemRunner"

include("test-systems:EMB:rest-ncs")
findProject(":test-systems:EMB:rest-ncs")?.name = "rest-ncs"

include("test-systems:Rest:SpringPayrollTestSystem")
findProject(":test-systems:Rest:SpringPayrollTestSystem")?.name = "SpringPayrollTestSystem"

