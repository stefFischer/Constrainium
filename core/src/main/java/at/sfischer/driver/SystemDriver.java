package at.sfischer.driver;

import at.sfischer.constraints.data.InOutputDataCollection;
import at.sfischer.constraints.data.SimpleDataCollection;

public interface SystemDriver {

    String getIdentifier();

    InOutputDataCollection execute(SimpleDataCollection input) throws DriverException;
}
