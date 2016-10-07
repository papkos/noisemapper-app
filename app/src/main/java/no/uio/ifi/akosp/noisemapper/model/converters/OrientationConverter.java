package no.uio.ifi.akosp.noisemapper.model.converters;

import android.text.TextUtils;

import org.greenrobot.greendao.converter.PropertyConverter;

import no.uio.ifi.akosp.noisemapper.Utils;
import no.uio.ifi.akosp.noisemapper.model.Orientation;

/**
 * Created on 2016.09.27..
 *
 * @author Ákos Pap
 */
public class OrientationConverter implements PropertyConverter<Orientation, String> {
    @Override
    public Orientation convertToEntityProperty(String databaseValue) {
        if (TextUtils.isEmpty(databaseValue)) {
            return null;
        }
        return Utils.orientationFromJson(databaseValue);
    }

    @Override
    public String convertToDatabaseValue(Orientation orientation) {
        return Utils.orientationToJson(orientation).toString();
    }
}
