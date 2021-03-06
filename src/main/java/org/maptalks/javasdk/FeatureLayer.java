package org.maptalks.javasdk;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.maptalks.gis.core.geojson.Feature;
import org.maptalks.gis.core.geojson.json.GeoJSONFactory;
import org.maptalks.javasdk.db.Layer;
import org.maptalks.javasdk.db.LayerField;
import org.maptalks.javasdk.exceptions.InvalidLayerException;
import org.maptalks.javasdk.exceptions.RestException;
import org.maptalks.javasdk.http.HttpRestClient;
import org.maptalks.javasdk.utils.ArrayUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 图层数据操作类
 * @author duscin
 *
 */
public class FeatureLayer extends Layer {
	private MapDatabase mapDatabase;
	private String restURL;

    /**
     * 构造函数
     *
     * @param id
     * @param mapDatabase
     * @throws IOException
     * @throws RestException
     * @throws InvalidLayerException
     */
	public FeatureLayer(final String id, final MapDatabase mapDatabase)
			throws IOException, RestException, InvalidLayerException {
		Layer layer =  mapDatabase.getLayer(id);
		if (layer == null) {
			throw new InvalidLayerException(
					"there is no layer with identifier:" + id);
		}
		this.setId(layer.getId());
		this.setName(layer.getName());
        this.setFields(layer.getFields());
        this.setSource(layer.getSource());
        this.setType(layer.getType());
        this.setSymbolConfig(layer.getSymbolConfig());
		this.mapDatabase = mapDatabase;
		this.restURL = mapDatabase.dbRestURL;
	}

	/**
	 * 默认的构造函数
	 */
	protected FeatureLayer() {
	}

	/**
	 * 添加图层自定义属性列
	 * @param field	欲添加的自定义属性列
	 * @throws IOException
	 * @throws RestException
	 */
	public void addLayerField(LayerField field) throws IOException,
			RestException {
		if (field == null)
			return;
		final String url = this.restURL + "layers/"+this.getId()+"/fields?op=create";
		final Map<String, String> param = new HashMap<String, String>();
		param.put("data", JSON.toJSONString(field));
		HttpRestClient.doPost(url, param, this.mapDatabase.isUseGZIP());
		return;
	}

	/**
	 * 修改图层自定义属性列
	 * @param fieldName 原属性名
	 * @param field	新的自定义属性定�?
	 * @throws IOException
	 * @throws RestException
	 */
	public void updateLayerField(String fieldName, LayerField field)
			throws IOException, RestException {
		if (fieldName == null
				|| fieldName.trim().length() == 0
				|| field == null)
			return;
		final String url = this.restURL + "layers/"+this.getId()+"/fields/"+fieldName+"?op=update";
		final Map<String, String> param = new HashMap<String, String>();
		param.put("data", JSON.toJSONString(field));
		HttpRestClient.doPost(url, param, this.mapDatabase.isUseGZIP());
		return;
	}

	/**
	 * 删除图层自定义属性列
     * 对于某些数据库, 属性列存在数据或其他情况下, 属性列可能无法被删除
	 * @param fieldName	属性名
	 * @throws IOException
	 * @throws RestException
	 */
	public void removeLayerField(String fieldName) throws IOException,
			RestException {
		if (fieldName == null || fieldName.trim().length() == 0) {
            return;
        }
		final String url = this.restURL + "layers/"+this.getId()+"/fields/"+fieldName+"?op=remove";
		HttpRestClient.doPost(url, null, this.mapDatabase.isUseGZIP());
		return;
	}

	/**
	 * 取得图层的自定义属性列
	 * @return
	 * @throws IOException
	 * @throws RestException
	 */
	public List<LayerField> getLayerFields() throws IOException, RestException {
		final String url = this.restURL + "layers/"+this.getId()+"/fields";
		final List<LayerField> rest = HttpRestClient.doParseGet(url, null,
				LayerField.class, this.mapDatabase.isUseGZIP());
		if (rest == null || rest.size() == 0)
			return null;
		return rest;
	}

	/**
	 * 添加 Feature
	 * 
	 * @param feature
	 * @throws RestException
	 * @throws IOException
	 */
	public void add(final Feature feature) throws IOException,
			RestException {
		if (feature == null)
			throw new RestException(ErrorCodes.ERRCODE_ILLEGAL_ARGUMENT,
					"Geometry  is null");
		final String url = this.restURL + "layers/"+this.getId()+"/data?op=create";
		postRequest(url, toJSONString(feature));
	}

	/**
	 * 批量添加 Feature
	 * 
	 * @param features
	 * @throws IOException
	 * @throws RestException
	 */
	public void add(final List<Feature> features) throws IOException,
			RestException {
		if (features == null)
			throw new RestException(ErrorCodes.ERRCODE_ILLEGAL_ARGUMENT,
					"Geometry  is null");
		final String url = this.restURL + "layers/"+this.getId()+"/data?op=create";
		postRequest(url, toJSONString(features));
	}

	/**
	 * 更新符合条件的 Feature
	 *
     * @param condition
	 * @param feature
	 * @throws RestException
	 * @throws IOException
	 */
	public void update(String condition, final Feature feature)
			throws IOException, RestException {
		if (feature == null
				|| feature.getId() == null) {
			throw new RestException(ErrorCodes.ERRCODE_ILLEGAL_ARGUMENT,
					"Geometry identifier is null");
		}
		final String url = this.restURL + "layers/"+this.getId()+"/data?op=update";
		final Map<String, String> params = new HashMap<String, String>();
		params.put("data", toJSONString(feature));
        params.put("condition", condition);
		HttpRestClient.doPost(url, params, this.mapDatabase.isUseGZIP());
	}

	public static String toJSONString(final Object obj) {
		if (obj == null) {
            return null;
        }
		if (obj instanceof String) {
            return (String) obj;
        }
		final SerializerFeature[] features = { SerializerFeature.DisableCircularReferenceDetect };
		return JSON.toJSONString(obj, features);
	}

	/**
	 * 删除符合查询条件的Feature
     *
	 * @param condition
	 * @throws RestException 
	 * @throws IOException 
	 */
	public void remove(String condition)
			throws IOException, RestException {
		if (condition == null || condition.trim().length() == 0) {
			return;
		}
		final String url = this.restURL + "layers/"+this.getId()+"/data?op=remove";
        final Map<String, String> params = new HashMap<String, String>();
        params.put("condition", condition);
		HttpRestClient.doPost(url, params, this.mapDatabase.isUseGZIP());
	}

	/**
	 * 删除图层表中所有数据
	 * 
	 * @throws IOException
	 * @throws RestException
	 */
	public void removeAll() throws IOException, RestException {
		final String url = this.restURL + "layers/"+this.getId()+"/data?op=removeAll";
		postRequest(url, null);

	}


	/**
	 * 批量修改图层的自定义属性数据
	 * @param condition 查询条件
	 * @param properties 新的自定义属性数据
	 * @throws IOException
	 * @throws RestException
	 */
	public void updateProperties(String condition, Object properties) throws IOException,
			RestException {
		if (properties == null) {
			return;
		}
        final String url = this.restURL + "layers/"+this.getId()+"/data?op=update";
        final Map<String, String> params = new HashMap<String, String>();
        params.put("condition", condition);
		params.put("data", toJSONString(properties));

		HttpRestClient.doPost(url, params, this.mapDatabase.isUseGZIP());
	}

	private void postRequest(final String url, final String data)
			throws IOException, RestException {
		final Map<String, String> params = new HashMap<String, String>();
		params.put("data", data);
		HttpRestClient.doPost(url, params, this.mapDatabase.isUseGZIP());
	}


	/**
	 * 查询符合条件的数据
	 * @param queryFilter
	 * @param page 第几页
	 * @param count 每页结果数
	 * @return
	 * @throws IOException
	 * @throws RestException
	 */
	public Feature[] query(QueryFilter queryFilter, int page, int count)
			throws IOException, RestException {
        if (queryFilter == null) {
            queryFilter = new QueryFilter();
        }
        final String json = queryJson(queryFilter, page, count);
        if (json == null || json.length() == 0) {
            return null;
        }
        return GeoJSONFactory.createFeatureArray(json);
	}


	/**
	 * 查询Geometry，但返回的是geometry的json形式，用于只需要返回Json的场景，速度较快
	 * @param queryFilter
     * @param page
     * @param count
	 * @return
	 * @throws IOException
	 * @throws RestException
	 */
	public String queryJson(QueryFilter queryFilter, int page, int count)
			throws IOException, RestException {
        if (page < 0 || count <= 0) {
            return null;
        }
        if (queryFilter == null) {
            queryFilter = new QueryFilter();
        }
        final String url = this.restURL + "layers/"+this.getId()+"/data?op=query";

        final Map<String, String> params = FeatureLayer
                .prepareFilterParameters(queryFilter);
        params.put("page", page + "");
        params.put("count", count + "");

        return HttpRestClient.doPost(url, params, this.mapDatabase.isUseGZIP());
	}

	/**
	 * 查询符合条件的Feature数据的自定义属性，返回为自定义属性的json字符串，适用于只需要查询自定义属性数据的应用场景
	 * @param queryFilter
     * @param page
     * @param count
	 * @return
	 * @throws IOException
	 * @throws RestException
	 */
	public String queryProperties(QueryFilter queryFilter, int page,
			int count) throws IOException, RestException {
        if (page < 0 || count <= 0) {
            return null;
        }
        String url = this.restURL + "layers/"+this.getId()+"/data?op=queryAttributes";
        final Map<String, String> params = FeatureLayer
                .prepareFilterParameters(queryFilter);
        params.put("page", page + "");
        params.put("count", count + "");


        return HttpRestClient.doPost(url, params, this.mapDatabase.isUseGZIP());
	}

	/**
	 * 统计结果数
	 * @param queryFilter
	 * @return
	 * @throws IOException
	 * @throws RestException
	 */
	public long count(QueryFilter queryFilter) throws IOException,
			RestException {
        if (queryFilter == null) {
            queryFilter = new QueryFilter();
        }
        final String url = this.restURL + "layers/"+this.getId()+"/data?op=count";
        final Map<String, String> params = FeatureLayer
                .prepareFilterParameters(queryFilter);
        return Long.parseLong(HttpRestClient.doPost(url, params,
                this.mapDatabase.isUseGZIP()));
	}

	protected static Map<String, String> prepareFilterParameters(
			QueryFilter queryFilter) {
		if (queryFilter == null) {
			return new HashMap<String, String>();
		}
		final Map<String, String> params = new HashMap<String, String>();
		String attributeCond = queryFilter.getCondition();
		if (attributeCond != null && attributeCond.length() > 0) {
			params.put("attributeCond", attributeCond);
		}
		SpatialFilter spatialFilter = queryFilter.getSpatialFilter();
		if (spatialFilter != null && spatialFilter.getGeometry() != null) {
			params.put("spatialFilter", toJSONString(spatialFilter));
		}

		String retCoordinateType = queryFilter.getCoordinateType();
		if (retCoordinateType != null) {
			params.put("coordinateType", retCoordinateType.toString());
		}
		if (queryFilter.isWithSymbol()) {
			params.put("needsymbol", queryFilter.isWithSymbol() + "");
		}
		String[] fields = queryFilter.getResultFields();
		if (fields != null) {
			params.put("fields", ArrayUtils.join(fields));
		}

		return params;
	}
}
