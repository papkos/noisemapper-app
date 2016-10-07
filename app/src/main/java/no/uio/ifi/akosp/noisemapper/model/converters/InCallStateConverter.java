package no.uio.ifi.akosp.noisemapper.model.converters;

import org.greenrobot.greendao.converter.PropertyConverter;

import no.uio.ifi.akosp.noisemapper.model.InCallState;

/**
 * Created on 2016.09.27..
 *
 * @author Ákos Pap
 */
public class InCallStateConverter implements PropertyConverter<InCallState, String> {
    @Override
    public InCallState convertToEntityProperty(String databaseValue) {
        return InCallState.valueOf(databaseValue);
    }

    @Override
    public String convertToDatabaseValue(InCallState entityProperty) {
        return entityProperty.name();
    }
}
