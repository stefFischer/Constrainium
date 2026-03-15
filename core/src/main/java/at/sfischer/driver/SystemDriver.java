package at.sfischer.driver;

import at.sfischer.constraints.data.DataObject;
import at.sfischer.constraints.data.InOutputDataCollection;
import at.sfischer.constraints.data.SimpleDataCollection;

public interface SystemDriver {

    String getIdentifier();

    DataObject execute(DataObject input) throws DriverException;

    default InOutputDataCollection execute(SimpleDataCollection input) throws DriverException {
        InOutputDataCollection inout = new InOutputDataCollection();
        for (DataObject in : input.getDataCollection()) {
            DataObject out = this.execute(in);
            if(out != null){
                inout.addDataEntry(in, out);
            }
        }

        return inout;
    }
}
