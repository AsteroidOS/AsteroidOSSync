package org.freedesktop;

import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.Position;

/**
 * Auto-generated class.
 */
public class GetServerInformationTuple extends Tuple {
    @Position(0)
    private String _arg0;
    @Position(1)
    private String name;
    @Position(2)
    private String vendor;
    @Position(3)
    private String version;

    public GetServerInformationTuple(String _arg0, String name, String vendor, String version) {
        this._arg0 = _arg0;
        this.name = name;
        this.vendor = vendor;
        this.version = version;
    }

    public void setArg0(String arg) {
        _arg0 = arg;
    }

    public String getArg0() {
        return _arg0;
    }
    public void setName(String arg) {
        name = arg;
    }

    public String getName() {
        return name;
    }
    public void setVendor(String arg) {
        vendor = arg;
    }

    public String getVendor() {
        return vendor;
    }
    public void setVersion(String arg) {
        version = arg;
    }

    public String getVersion() {
        return version;
    }


}
