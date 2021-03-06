/*
 * Copyright (c) 2015 Villu Ruusmann
 *
 * This file is part of JPMML-SkLearn
 *
 * JPMML-SkLearn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JPMML-SkLearn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with JPMML-SkLearn.  If not, see <http://www.gnu.org/licenses/>.
 */
package sklearn.linear_model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Model;
import org.dmg.pmml.OpType;
import org.dmg.pmml.regression.RegressionModel;
import org.jpmml.converter.CMatrixUtil;
import org.jpmml.converter.CategoricalLabel;
import org.jpmml.converter.ContinuousLabel;
import org.jpmml.converter.Feature;
import org.jpmml.converter.ModelUtil;
import org.jpmml.converter.Schema;
import org.jpmml.converter.ValueUtil;
import org.jpmml.converter.mining.MiningModelUtil;
import org.jpmml.converter.regression.RegressionModelUtil;
import sklearn.Classifier;
import sklearn.ClassifierUtil;

public class LinearClassifier extends Classifier {

	public LinearClassifier(String module, String name){
		super(module, name);
	}

	@Override
	public int getNumberOfFeatures(){
		int[] shape = getCoefShape();

		return shape[1];
	}

	@Override
	public Model encodeModel(Schema schema){
		int[] shape = getCoefShape();

		int numberOfClasses = shape[0];
		int numberOfFeatures = shape[1];

		boolean hasProbabilityDistribution = hasProbabilityDistribution();

		List<? extends Number> coef = getCoef();
		List<? extends Number> intercepts = getIntercept();

		CategoricalLabel categoricalLabel = (CategoricalLabel)schema.getLabel();

		List<? extends Feature> features = schema.getFeatures();

		if(numberOfClasses == 1){
			ClassifierUtil.checkSize(2, categoricalLabel);

			return RegressionModelUtil.createBinaryLogisticClassification(features, ValueUtil.asDoubles(CMatrixUtil.getRow(coef, numberOfClasses, numberOfFeatures, 0)), ValueUtil.asDouble(intercepts.get(0)), RegressionModel.NormalizationMethod.LOGIT, hasProbabilityDistribution, schema);
		} else

		if(numberOfClasses >= 3){
			ClassifierUtil.checkSize(numberOfClasses, categoricalLabel);

			Schema segmentSchema = new Schema(new ContinuousLabel(null, DataType.DOUBLE), Collections.<Feature>emptyList());

			List<RegressionModel> regressionModels = new ArrayList<>();

			for(int i = 0, rows = categoricalLabel.size(); i < rows; i++){
				RegressionModel regressionModel = RegressionModelUtil.createRegression(features, ValueUtil.asDoubles(CMatrixUtil.getRow(coef, numberOfClasses, numberOfFeatures, i)), ValueUtil.asDouble(intercepts.get(i)), RegressionModel.NormalizationMethod.LOGIT, segmentSchema)
					.setOutput(ModelUtil.createPredictedOutput(FieldName.create("decisionFunction(" + categoricalLabel.getValue(i) + ")"), OpType.CONTINUOUS, DataType.DOUBLE));

				regressionModels.add(regressionModel);
			}

			return MiningModelUtil.createClassification(regressionModels, RegressionModel.NormalizationMethod.SIMPLEMAX, hasProbabilityDistribution, schema);
		} else

		{
			throw new IllegalArgumentException();
		}
	}

	public List<? extends Number> getCoef(){
		return getArray("coef_", Number.class);
	}

	public int[] getCoefShape(){
		return getArrayShape("coef_", 2);
	}

	public List<? extends Number> getIntercept(){
		return getArray("intercept_", Number.class);
	}
}