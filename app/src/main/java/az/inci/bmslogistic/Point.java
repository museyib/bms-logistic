package az.inci.bmslogistic;

public class Point
{
    double longitude;
    double latitude;

    public Point(double longitude, double latitude)
    {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public double getDistance(Point point)
    {
        double distance = 0;
        if(point == null)
            return distance;

        double radius = 6378.137;

        double dLon = (point.longitude - longitude) * Math.PI / 180;
        double dLat = (point.latitude - latitude) * Math.PI / 180;
        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(latitude * Math.PI / 180) *
                                                     Math.cos(
                                                             point.latitude * Math.PI / 180) * Math.pow(
                Math.sin(dLon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        distance = radius * c * 1000;

        return Math.round(distance);
    }
}
