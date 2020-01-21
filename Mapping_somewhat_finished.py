import math
import numpy as np
from sklearn.neighbors import NearestNeighbors
import matplotlib.pyplot as plt
from rplidar import RPLidar
import time

def prob_to_log_odds(p):
    """
    Convert proability values p to the corresponding log odds l.
    p could be a scalar or a matrix.
    """
    return np.log(p/(1-p))

PORT_NAME = 'COM6'
    

#Initial cell occupancy probability.
prior = 0.50
#Probabilities related to the laser range finder sensor model.
probOcc = 0.9
probFree = 0.35
#Map grid size in meters. Decrease for better resolution.
gridSize = 25                  # 250 = 250mm each side of the grid
#Set up map boundaries and initialize map
mapsize = 7000                 # 2000 = 2000mm each side of the rectangle
num_grids = (mapsize//gridSize)
#Used when updating the map.
logOddsPrior = prob_to_log_odds(prior)
#Initializing the map as ones
Map = logOddsPrior * np.ones((num_grids,num_grids))
#Initializing the robot state
robotState = np.array([[mapsize/2],[mapsize/2],[0]])

    
def update_map(readings): 
    mapUpdate = inv_sensor_model(readings)
    mapUpdate -= logOddsPrior * np.ones((num_grids,num_grids)) 
    return mapUpdate

def inv_sensor_model(readings):
    """
    Input:
        readings: an array Nx2 (N,2)
    Output:
        mapUpdate: an array num_grids x num_grids (num_grids,num_grids)
    """
    mapUpdate = np.zeros(Map.shape)
    robotStates = np.tile(robotState[:2,:],(1,len(readings))).T
    
    laserEndPoints = robotStates + laser_endpoints_as_cartesian(readings)
    laserEndPoints[laserEndPoints<0] = 0
    laserEndPoints[laserEndPoints>mapsize] = mapsize - 1
    
    xGridsMapFrame = laserEndPoints[:,0] // gridSize
    yGridsMapFrame = laserEndPoints[:,1] // gridSize
    GridsMapFrame = np.array([xGridsMapFrame,yGridsMapFrame])
    laserEndPointsMapFrame = GridsMapFrame.T
    robotGridsMapFrame = robotStates // gridSize
    
    for i in range(len(laserEndPoints)):
        points = bresenham(int(robotGridsMapFrame[i,0]),int(robotGridsMapFrame[i,1]),int(laserEndPointsMapFrame[i,0]), int(laserEndPointsMapFrame[i,1]))
        for point in points[:-1]:
            if point[0] < (num_grids) and point[1] < (num_grids):
                mapUpdate[point[1],point[0]] += prob_to_log_odds(probFree)
        if points[-1][1] < (num_grids) and points[-1][0] < (num_grids):
            mapUpdate[points[-1][1],points[-1][0]] += prob_to_log_odds(probOcc)

    return mapUpdate
    
def laser_endpoints_as_cartesian(readings):
    """
    Input:
        readings: a numpy array Nx2 (N,2)
                [[theta_1,r_1],
                 [theta_2,r_2],
                     ...,
                 [theta_n,r_n]]
    Output:
        readings as cartesian: a numpy array Nx2 (N,2)
                [[x_1,y_1],
                 [x_2,y_2],
                     ...,
                 [x_n,y_n]]
    """
    endpoints = np.zeros(readings.shape)
    for index in range(readings.shape[0]):
        endpoints[index,:] = polar_to_cartesian(-readings[index][0],readings[index][1])
    return endpoints

def laser_endpoints_as_polar(readings):
    """
    Input:
        readings: a numpy array Nx2 (N,2)
                [[x_1,y_1],
                 [x_2,y_2],
                     ...,
                 [x_n,y_n]]
    Output:
        readings as polar: a numpy array Nx2 (N,2)
                [[theta_1,r_1],
                 [theta_2,r_2],
                     ...,
                 [theta_n,r_n]]
    """
    readings_as_polar = np.zeros(readings.shape)
    for index in range(readings.shape[0]):
        readings_as_polar[index,:] = cartesian_to_polar(-readings[index][0],readings[index][1])
    return readings_as_polar

def polar_to_cartesian(theta, distance):
    return np.array([distance * np.cos(np.deg2rad(theta)), distance * np.sin(np.deg2rad(theta))])

def cartesian_to_polar(x,y):
    return np.array([np.rad2deg(np.arctan2(y,x)+np.pi),np.sqrt(x**2+y**2)])

def log_odds_to_prob(l):
    """
    Convert log odds l to the corresponding probability values p.
    l could be a scalar or a matrix.
    """
    return 1 - 1 / (1 + np.exp(l))
    
def euclidean_distance(point1, point2):
    """
    Euclidean distance between two points.
    :param point1: the first point as a tuple (a_1, a_2, ..., a_n)
    :param point2: the second point as a tuple (b_1, b_2, ..., b_n)
    :return: the Euclidean distance
    """
    a = np.array(point1)
    b = np.array(point2)

    return np.linalg.norm(a - b, ord=2)


def point_based_matching(point_pairs):
    """
    This function is based on the paper "Robot Pose Estimation in Unknown Environments by Matching 2D Range Scans"
    by F. Lu and E. Milios.

    :param point_pairs: the matched point pairs [((x1, y1), (x1', y1')), ..., ((xi, yi), (xi', yi')), ...]
    :return: the rotation angle and the 2D translation (x, y) to be applied for matching the given pairs of points
    """

    x_mean = 0
    y_mean = 0
    xp_mean = 0
    yp_mean = 0
    n = len(point_pairs)

    if n == 0:
        return None, None, None

    for pair in point_pairs:

        (x, y), (xp, yp) = pair

        x_mean += x
        y_mean += y
        xp_mean += xp
        yp_mean += yp

    x_mean /= n
    y_mean /= n
    xp_mean /= n
    yp_mean /= n

    s_x_xp = 0
    s_y_yp = 0
    s_x_yp = 0
    s_y_xp = 0
    for pair in point_pairs:

        (x, y), (xp, yp) = pair

        s_x_xp += (x - x_mean)*(xp - xp_mean)
        s_y_yp += (y - y_mean)*(yp - yp_mean)
        s_x_yp += (x - x_mean)*(yp - yp_mean)
        s_y_xp += (y - y_mean)*(xp - xp_mean)

    rot_angle = math.atan2(s_x_yp - s_y_xp, s_x_xp + s_y_yp)
    translation_x = xp_mean - (x_mean*math.cos(rot_angle) - y_mean*math.sin(rot_angle))
    translation_y = yp_mean - (x_mean*math.sin(rot_angle) + y_mean*math.cos(rot_angle))

    return rot_angle, translation_x, translation_y


def icp(reference_points, points, max_iterations=100, distance_threshold=1000, convergence_translation_threshold=1e-3,
        convergence_rotation_threshold=1e-4, point_pairs_threshold=100, verbose=False):
    """
    An implementation of the Iterative Closest Point algorithm that matches a set of M 2D points to another set
    of N 2D (reference) points.

    :param reference_points: the reference point set as a numpy array (N x 2)
    :param points: the point that should be aligned to the reference_points set as a numpy array (M x 2)
    :param max_iterations: the maximum number of iteration to be executed
    :param distance_threshold: the distance threshold between two points in order to be considered as a pair
    :param convergence_translation_threshold: the threshold for the translation parameters (x and y) for the
                                              transformation to be considered converged
    :param convergence_rotation_threshold: the threshold for the rotation angle (in rad) for the transformation
                                               to be considered converged
    :param point_pairs_threshold: the minimum number of point pairs the should exist
    :param verbose: whether to print informative messages about the process (default: False)
    :return: the transformation history as a list of numpy arrays containing the rotation (R) and translation (T)
             transformation in each iteration in the format [R | T] and the aligned points as a numpy array M x 2
    """

    transformation_history = []

    nbrs = NearestNeighbors(n_neighbors=1, algorithm='kd_tree').fit(reference_points)
    
    rotation_sum = 0
    translation_x_sum = 0
    translation_y_sum = 0
    
    for iter_num in range(max_iterations):
        if verbose:
            print('------ iteration', iter_num, '------')

        closest_point_pairs = []  # list of point correspondences for closest point rule
        distances, indices = nbrs.kneighbors(points)
        
        for nn_index in range(len(distances)):
            if distances[nn_index][0] < distance_threshold:
                closest_point_pairs.append((points[nn_index], reference_points[indices[nn_index][0]]))

        # if only few point pairs, stop process
        if verbose:
            print('number of pairs found:', len(closest_point_pairs))
        if len(closest_point_pairs) < point_pairs_threshold:
            if verbose:
                print('No better solution can be found (very few point pairs)!')
            break

        # compute translation and rotation using point correspondences
        closest_rot_angle, closest_translation_x, closest_translation_y = point_based_matching(closest_point_pairs)
        if closest_rot_angle is not None:
            rotation_sum += math.degrees(closest_rot_angle)
            translation_x_sum += closest_translation_x
            translation_y_sum += closest_translation_y
            if verbose:
                print('Rotation:', math.degrees(closest_rot_angle), 'degrees')
                print('Translation:', closest_translation_x, closest_translation_y)
        if closest_rot_angle is None or closest_translation_x is None or closest_translation_y is None:
            if verbose:
                print('No better solution can be found!')
            break

        # transform 'points' (using the calculated rotation and translation)
        c, s = math.cos(closest_rot_angle), math.sin(closest_rot_angle)
        rot = np.array([[c, -s],
                        [s, c]])
        aligned_points = np.dot(points, rot.T)
        aligned_points[:, 0] += closest_translation_x
        aligned_points[:, 1] += closest_translation_y

        # update 'points' for the next iteration
        points = aligned_points

        # update transformation history
        transformation_history.append(np.hstack((rot, np.array([[closest_translation_x], [closest_translation_y]]))))
        
        # check convergence
        if (abs(closest_rot_angle) < convergence_rotation_threshold) \
                and (abs(closest_translation_x) < convergence_translation_threshold) \
                and (abs(closest_translation_y) < convergence_translation_threshold):
            if verbose:
                print('Converged!')
            break

    return (translation_x_sum, translation_y_sum, rotation_sum), points

def bresenham(x1, y1, x2, y2):
    """Bresenham's Line Algorithm
    Produces a list of tuples from start and end
    >>> points1 = get_line((0, 0), (3, 4))
    >>> points2 = get_line((3, 4), (0, 0))
    >>> assert(set(points1) == set(points2))
    >>> print points1
    [(0, 0), (1, 1), (1, 2), (2, 3), (3, 4)]
    >>> print points2
    [(3, 4), (2, 3), (1, 2), (1, 1), (0, 0)]
    """
    # Setup initial conditions
    dx = x2 - x1
    dy = y2 - y1

    # Determine how steep the line is
    is_steep = abs(dy) > abs(dx)

    # Rotate line
    if is_steep:
        x1, y1 = y1, x1
        x2, y2 = y2, x2

    # Swap start and end points if necessary and store swap state
    swapped = False
    if x1 > x2:
        x1, x2 = x2, x1
        y1, y2 = y2, y1
        swapped = True

    # Recalculate differentials
    dx = x2 - x1
    dy = y2 - y1

    # Calculate error
    error = int(dx / 2.0)
    ystep = 1 if y1 < y2 else -1

    # Iterate over bounding box generating points between start and end
    y = y1
    points = []
    for x in range(x1, x2 + 1):
        coord = (y, x) if is_steep else (x, y)
        points.append(coord)
        error -= abs(dy)
        if error < 0:
            y += ystep
            error += dx

    # Reverse the list if the coordinates were swapped
    if swapped:
        points.reverse()
    return points

def measure(measurment_num = 1):
    '''
    Input:
        buffer_size: 
    '''
    lidar.connect()
    if measurment_num == 1:
        lidar.stop()
        lidar.disconnect()
        return np.array(next(lidar.iter_measurments())[-2:])
    else:
        data = []
        t = measurment_num
        for measurment in lidar.iter_measurments():
            data.append(measurment[-2:])
            t -= 1
            if t == 0:
                break
        lidar.stop()
        lidar.disconnect()
        return np.array(data)



def plotMap(Map, boundary = 0.95, binary = False):
    if binary:
        Map = (Map <= boundary).astype(int)
    else:
        # -Map because otherwise the occupied cells would be white and the free ones black, i want the opposite
        Map = -Map
    Map = log_odds_to_prob(Map)
    im = Map.reshape(num_grids, num_grids)
    plt.gray()
    plt.imshow(im)

def update_position(transformation,state):
    '''
        Input:
            transformation: x, y translation and theta rotation as a result of icp (tuple)
            state: current robot state (x,y,theta) (numpy array) 3x1
    '''
    x,y,theta = transformation
    updated_state = state + np.array([(x,y,theta)]).T
    updated_state[:2][updated_state[:2] > mapsize] = mapsize
    updated_state[:2][updated_state[:2] < 0] = 0
    updated_state[2][updated_state[2] > 360] = updated_state[2]%360
    updated_state[2][updated_state[2] < -360] = -(-updated_state[2]%360)
    return updated_state

def obstacles_on_map(Map, threshold = 0.99):
    x, y = np.where(Map >= threshold)
    xy = np.concatenate((np.array([x]),np.array([y])), axis = 0).T
    return xy

if __name__ == "__main__":
    lidar = RPLidar(PORT_NAME)
    
#    for i in range(50):
#        readings = measure(measurment_num = 20)
#        lidar.stop()
#        lidar.disconnect()
#        Map += update_map(readings)
#        lidar.connect()
#        print(i)
        
#    plotMap(Map, binary = 0)
    
#    plt.cla()
    

    




#    plt.scatter(new_points[:,0],new_points[:,1])
#    plt.scatter(readings[:,0],readings[:,1])
#    plt.scatter(readings1[:,0],readings1[:,1])

    previous_measurment = measure(measurment_num = 360)
    previous_measurment = previous_measurment[previous_measurment.all(1)]
    Map += update_map(previous_measurment)
    previous_measurment = laser_endpoints_as_cartesian(previous_measurment)
    occupied_cells = obstacles_on_map(Map)

    for i in range(25):
        readings = measure(measurment_num = 360)
        readings = readings[readings.all(1)]
        readings_as_cartesian = laser_endpoints_as_cartesian(readings)
        
        if i == 4:
            a = previous_measurment
            b = readings_as_cartesian
        transformation, new_points = icp(previous_measurment,readings_as_cartesian)
#        robotState = update_position(transformation,robotState)
        new_points_as_polar = laser_endpoints_as_polar(new_points)
        
        Map += update_map(new_points_as_polar)
        occupied_cells = obstacles_on_map(Map)
        previous_measurment = new_points
        
        print(i)
        print(transformation)
        
    plotMap(Map,binary = 0)


#    readings = np.array([[np.random.rand()*360,1500] for i in range(4000)])

#    icp()
#    update_position()

#    Map[num_grids//2,num_grids//2] = Map[(num_grids//2)+25,(num_grids//2)+25]
    
#    plotMap(Map,binary = True)


    
