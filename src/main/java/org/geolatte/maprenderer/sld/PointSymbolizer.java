/*
 * This file is part of the GeoLatte project.
 *
 *     GeoLatte is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     GeoLatte is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with GeoLatte.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright (C) 2010 - 2011 and Ownership of code is shared by:
 *  Qmino bvba - Esperantolaan 4 - 3001 Heverlee  (http://www.qmino.com)
 *  Geovise bvba - Generaal Eisenhowerlei 9 - 2140 Antwerpen (http://www.geovise.com)
 */

package org.geolatte.maprenderer.sld;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import net.opengis.se.v_1_1_0.GraphicType;
import net.opengis.se.v_1_1_0.PointSymbolizerType;
import org.geolatte.maprenderer.map.MapGraphics;
import org.geolatte.maprenderer.sld.graphics.*;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.io.IOException;

/**
 * A Symbolizer for point features.
 *
 * <p>See SE §11.3 </p>
 * <p>Note: the SE spec states on page 23 that Graphic elements can occur multiple times, but the XSD schema
 * specifies that it can occur at most one time. Here the XSD schema is followed.</p>
 */
public class PointSymbolizer extends AbstractSymbolizer {


    final private String geometryProperty;
    final private Graphic graphic;

    //TODO -- do this by injection
    //TODO -- configure or search for graphics packages.
    final private ExternalGraphicsRepository graphicsRepository = new ExternalGraphicsRepository(
            new String[]{"graphics"}
    );


    public PointSymbolizer(PointSymbolizerType type) {
        super(type);
        this.geometryProperty = readGeometry(type.getGeometry());
        this.graphic = readGraphic(type.getGraphic());
    }

    @Override
    public void symbolize(MapGraphics graphics, Geometry geometry) {
        Point point = getPoint(geometry);
        symbolize(graphics, point, graphic);
    }

    private void symbolize(MapGraphics graphics, Point point, Graphic graphic) {
        for (MarkOrExternalGraphicHolder holder: graphic.getSources()){
            try {
                if (symbolize(graphics, point, graphic, holder)) return;
            } catch (GraphicDrawException e) {
                //TODO -- check exception handling policy.
                throw new RuntimeException(e);
            }
        }
    }

    private boolean symbolize(MapGraphics graphics, Point point, Graphic graphic, MarkOrExternalGraphicHolder holder)
            throws GraphicDrawException {
        boolean success;
        if (holder.isExternalGraphic()) {
            success = symbolize(graphics, point, graphic, holder.getExternalGrapic());
        } else {
            success = symbolize(graphics, point, graphic, holder.getMark());
        }
        return success;
    }

    private boolean symbolize(MapGraphics graphics, Point point, Graphic graphic, Mark mark) {
        return false; //TODO implement!
    }

    private boolean symbolize(MapGraphics graphics, Point point, Graphic graphic, ExternalGraphic externalGrapic) throws
            GraphicDrawException {
        GraphicSource gs = null;
        try {
            gs = graphicsRepository.get(externalGrapic.getUrl());
        } catch (IOException e) {
            throw new GraphicDrawException(e);
        }
        if (gs instanceof RenderedImageGraphicSource) {
            AffineTransform currentTransform = graphics.getTransform();
            RenderedImage image = (RenderedImage) gs.getGraphic();
            graphics.setTransform(new AffineTransform());
            try {
                AffineTransform pointTransform = getPointTransform(currentTransform, image, graphic);
                Point2D dstPnt = determineAnchorPoint(point, pointTransform);
                graphics.drawImage((Image)image, (int)dstPnt.getX(), (int)dstPnt.getY(), (ImageObserver)null);
            } finally {
                graphics.setTransform(currentTransform);
            }
        }
        return true;
    }

    /**
     * Determines the transform of the anchorpoint from its geographic location to a pixel in the graphics' device
     * space.
     *
     * <p>Not that SE specifies the anchorpoint in a coordinate system with origin in the lower-left
     * corner of the image, while java.awt.Graphics uses the top-left as origin</p>
     */
    private AffineTransform getPointTransform(AffineTransform currentTransform, RenderedImage img, Graphic graphic) {
        AffineTransform applyAnchorPoint = new AffineTransform();
        Point2D anchorPoint = graphic.getAnchorPoint();
        applyAnchorPoint.setToTranslation(
                - anchorPoint.getX() * img.getWidth(),
                - (1- anchorPoint.getY()) * img.getHeight());
        applyAnchorPoint.concatenate(currentTransform);
        return applyAnchorPoint;
    }

    private Point2D determineAnchorPoint(Point point, AffineTransform transform)  {
        return transform.transform(new Point2D.Double(point.getX(), point.getY()), null);
    }

    private Point getPoint(Geometry geometry) {
        if (geometry instanceof Point) {
            return ((Point) geometry);
        }
        return geometry.getCentroid();
    }

    public String getGeometryProperty() {
        return geometryProperty;
    }

    private Graphic readGraphic(GraphicType graphicType) {
        return new Graphic(graphicType);
    }


    public Graphic getGraphic() {
        return this.graphic;
    }
}
