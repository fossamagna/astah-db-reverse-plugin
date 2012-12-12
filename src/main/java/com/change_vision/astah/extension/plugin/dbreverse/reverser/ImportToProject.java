package com.change_vision.astah.extension.plugin.dbreverse.reverser;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.change_vision.astah.extension.plugin.dbreverse.Messages;
import com.change_vision.astah.extension.plugin.dbreverse.reverser.model.ERRelationshipInfo;
import com.change_vision.astah.extension.plugin.dbreverse.reverser.model.TableInfo;
import com.change_vision.astah.extension.plugin.dbreverse.util.Constants;
import com.change_vision.astah.extension.plugin.dbreverse.util.DBReverseUtil;
import com.change_vision.astah.extension.plugin.dbreverse.view.DBComboBox;
import com.change_vision.astah.extension.plugin.dbreverse.view.ReverseDialog;
import com.change_vision.astah.extension.plugin.dbreverse.view.SchemaComboBox;
import com.change_vision.jude.api.inf.exception.InvalidEditingException;
import com.change_vision.jude.api.inf.exception.LicenseNotFoundException;
import com.change_vision.jude.api.inf.exception.ProjectLockedException;
import com.change_vision.jude.api.inf.exception.ProjectNotFoundException;
import com.change_vision.jude.api.inf.model.IModel;
import com.change_vision.jude.api.inf.project.ProjectAccessor;
import com.change_vision.jude.api.inf.project.ProjectAccessorFactory;

public class ImportToProject {

	private static final Logger logger = LoggerFactory.getLogger(ImportToProject.class);

	private static DBReader dbReader = null;

	public static void doImport() {
		dbReader = DBReader.getInstance();
		if (dbReader == null) {
			DBReverseUtil.showMessage(Messages.getMessage("message.database.disconnected"));
			return;
		}

		DBReverseUtil.showMessage(Messages.getMessage("message.database.importing"));
		go();
	}

	private static void go() {
        startImport();
	}

	private static void startImport() {
		String dbName = null;
		Object item = SchemaComboBox.getInstance().getSelectedItem();
		if (item != null) {
			dbName = item.toString();
		}

		String temporaryProjectFilePath = null;
		try {
			temporaryProjectFilePath = getTemporaryProjectFilePath();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

		try {
			String currentDBType = DBComboBox.getInstance().getSelectedItem().toString();
			List<TableInfo> tables = null;
			List<ERRelationshipInfo> relationships = null;
			if (Constants.MYSQL.equalsIgnoreCase(currentDBType)
					|| Constants.MSSQLSERVER.equalsIgnoreCase(currentDBType)
					|| Constants.OTHERS_CATEGORY.equalsIgnoreCase(currentDBType)) {
				tables = dbReader.getTables(dbName, null);
				relationships = dbReader.getRelationships(dbName, null);
			} else {
				tables = dbReader.getTables(null, dbName);
				relationships = dbReader.getRelationships(null, dbName);
			}

			ProjectAccessor projectAccessor = ProjectAccessorFactory.getProjectAccessor();
			String currentProjectPath = null;
			try {
                currentProjectPath = projectAccessor.getProjectPath();
            } catch (ProjectNotFoundException e) {
            }

			ProgressMonitor monitor = new ProgressMonitor() {
                @Override
                public void showMessage(String message) {
                    DBReverseUtil.showMessage(message);
                    
                }
            };
            DBToProject dbtj = new DBToProject(monitor );
            projectAccessor.create(temporaryProjectFilePath);
            IModel temporaryModel = projectAccessor.getProject();
			dbtj.importToProject(temporaryModel, tables, relationships);

			ReverseDialog.getInstance().setTemporaryProjectFilePath(temporaryProjectFilePath);
			ReverseDialog.getInstance().setVisible(false);
			
			if(currentProjectPath != null){
    			projectAccessor.addProjectEventListener(ReverseDialog.getInstance());
    			projectAccessor.open(currentProjectPath);
			    
			}


			DBReverseUtil.showMessage(Messages.getMessage("message.import.successfully"));

		} catch (LicenseNotFoundException e) {
			logger.error(e.getMessage(), e);
			DBReverseUtil.showErrorMessage(e);
		} catch (ProjectLockedException e) {
			logger.error(e.getMessage(), e);
			DBReverseUtil.showErrorMessage(e);
		} catch(InvalidEditingException e) {
			logger.error(e.getMessage(), e);
			DBReverseUtil.showErrorMessage(e);
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
			DBReverseUtil.showErrorMessage(e);
		}
	}

	private static String getTemporaryProjectFilePath() throws IOException {
		Calendar cal = Calendar.getInstance();
		StringBuilder tempFileName = new StringBuilder(Integer.toString(cal.get(Calendar.YEAR)))
		.append(Integer.toString(cal.get(Calendar.MONTH) + 1))
		.append(Integer.toString(cal.get(Calendar.DATE)))
		.append(Integer.toString(cal.get(Calendar.HOUR_OF_DAY)))
		.append(Integer.toString(cal.get(Calendar.MINUTE)))
		.append(Integer.toString(cal.get(Calendar.SECOND)));
		File tempFile = File.createTempFile(tempFileName.toString(), ".asta");
		tempFile.deleteOnExit();
		return tempFile.getAbsolutePath();
	}
}