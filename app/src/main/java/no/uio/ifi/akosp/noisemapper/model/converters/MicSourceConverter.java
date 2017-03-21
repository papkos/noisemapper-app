package no.uio.ifi.akosp.noisemapper.model.converters;

import org.greenrobot.greendao.converter.PropertyConverter;

import no.uio.ifi.akosp.noisemapper.model.MicSource;

/**
 * Created on 2016.09.27..
 *
 * @author Ákos Pap
 */
public class MicSourceConverter implements PropertyConverter<MicSource, String> {
    @Override
    public MicSource convertToEntityProperty(String databaseValue) {
        return MicSource.valueOf(databaseValue);
    }

    @Override
    public String convertToDatabaseValue(MicSource entityProperty) {
        return entityProperty.name();
    }
}
