package org.freedesktop;

import java.util.List;
import java.util.Map;
import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

/**
 * Auto-generated class.
 */
public class GetNotificationsStruct extends Struct {
    @Position(0)
    private final String member0;
    @Position(1)
    private final UInt32 member1;
    @Position(2)
    private final String member2;
    @Position(3)
    private final String member3;
    @Position(4)
    private final List<String> member4;
    @Position(5)
    private final Map<String, Variant<?>> member5;
    @Position(6)
    private final int member6;

    public GetNotificationsStruct(String member0, UInt32 member1, String member2, String member3, List<String> member4, Map<String, Variant<?>> member5, int member6) {
        this.member0 = member0;
        this.member1 = member1;
        this.member2 = member2;
        this.member3 = member3;
        this.member4 = member4;
        this.member5 = member5;
        this.member6 = member6;
    }


    public String getMember0() {
        return member0;
    }

    public UInt32 getMember1() {
        return member1;
    }

    public String getMember2() {
        return member2;
    }

    public String getMember3() {
        return member3;
    }

    public List<String> getMember4() {
        return member4;
    }

    public Map<String, Variant<?>> getMember5() {
        return member5;
    }

    public int getMember6() {
        return member6;
    }


}
