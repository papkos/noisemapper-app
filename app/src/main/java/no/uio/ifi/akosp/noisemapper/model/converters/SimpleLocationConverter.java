package no.uio.ifi.akosp.noisemapper.model.converters;

import android.text.TextUtils;

import org.greenrobot.greendao.converter.PropertyConverter;

import no.uio.ifi.akosp.noisemapper.Utils;
import no.uio.ifi.akosp.noisemapper.model.SimpleLocation;

/**
 * Created on 2016.09.27..
 *
 * @author Ákos Pap
 */
public class SimpleLocationConverter implements PropertyConverter<SimpleLocation, String> {
    @Override
    public SimpleLocation convertToEntityProperty(String databaseValue) {
        if (TextUtils.isEmpty(databaseValue)) {
            return null;
        }
        return Utils.simpleLocationFromJson(databaseValue);
    }

    @Override
    public String convertToDatabaseValue(SimpleLocation location) {
        return Utils.simpleLocationToJson(location).toString();
    }
}
