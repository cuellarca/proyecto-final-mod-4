package com.nurtricenter.logisticdelivery.infrastructure.persistence;

import com.nurtricenter.logisticdelivery.domain.shared.Geolocalizacion;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

/**
 * Traduce entre la {@code Geolocalizacion} del dominio y el {@code Point} de JTS que
 * Hibernate Spatial persiste como {@code geometry(Point,4326)}.
 *
 * <p>Convencion PostGIS/GeoJSON: el eje X es la longitud y el eje Y la latitud.</p>
 */
public final class GeoSupport {

    public static final int SRID_WGS84 = 4326;

    private static final GeometryFactory FACTORY =
            new GeometryFactory(new PrecisionModel(), SRID_WGS84);

    private GeoSupport() {
    }

    /** Geolocalizacion (dominio) -> Point JTS con SRID 4326. */
    public static Point toPoint(Geolocalizacion geo) {
        Point point = FACTORY.createPoint(new Coordinate(geo.lon(), geo.lat()));
        point.setSRID(SRID_WGS84);
        return point;
    }

    /** Point JTS -> Geolocalizacion (dominio). */
    public static Geolocalizacion toGeolocalizacion(Point point) {
        return new Geolocalizacion(point.getY(), point.getX());
    }
}
