/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.struts;

import com.liferay.portal.NoSuchLayoutException;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.rule.Sync;
import com.liferay.portal.kernel.test.rule.SynchronousDestinationTestRule;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Layout;
import com.liferay.portal.model.PortletInstance;
import com.liferay.portal.model.impl.VirtualLayout;
import com.liferay.portal.security.permission.PermissionChecker;
import com.liferay.portal.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.test.rule.MainServletTestRule;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortletKeys;
import com.liferay.portal.util.test.LayoutTestUtil;
import com.liferay.portlet.blogs.model.BlogsEntry;
import com.liferay.portlet.blogs.service.BlogsEntryLocalServiceUtil;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.mock.web.MockHttpServletRequest;

/**
 * @author Julio Camarero
 * @author Laszlo Csontos
 * @author Eduardo Garcia
 */
@Sync
public class FindActionTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(), MainServletTestRule.INSTANCE,
			SynchronousDestinationTestRule.INSTANCE);

	@Test
	public void testGetPlidAndPortletIdViewInContext() throws Exception {
		addLayouts(true, false);

		Object[] plidAndPorltetId = BaseFindActionHelper.getPlidAndPortletId(
			getThemeDisplay(), _blogsEntry.getGroupId(), _assetLayout.getPlid(),
			_PORTLET_IDS);

		Assert.assertEquals(_blogLayout.getPlid(), plidAndPorltetId[0]);
		Assert.assertEquals(PortletKeys.BLOGS, plidAndPorltetId[1]);
	}

	@Test
	public void testGetPlidAndPortletIdWhenPortletDoesNotExist()
		throws Exception {

		addLayouts(false, false);

		try {
			BaseFindActionHelper.getPlidAndPortletId(
				getThemeDisplay(), _blogsEntry.getGroupId(),
				_assetLayout.getPlid(), _PORTLET_IDS);

			Assert.fail();
		}
		catch (NoSuchLayoutException nsle) {
		}
	}

	@Test
	public void testSetTargetGroupWithDifferentGroup() throws Exception {
		addLayouts(true, true);

		HttpServletRequest request = getHttpServletRequest();

		BaseFindActionHelper.setTargetLayout(
			request, _blogsEntry.getGroupId(), _blogLayout.getPlid());

		Layout layout = (Layout)request.getAttribute(WebKeys.LAYOUT);

		Assert.assertTrue(layout instanceof VirtualLayout);
		Assert.assertNotEquals(_group.getGroupId(), layout.getGroupId());
	}

	@Test
	public void testSetTargetGroupWithSameGroup() throws Exception {
		addLayouts(true, false);

		HttpServletRequest request = getHttpServletRequest();

		BaseFindActionHelper.setTargetLayout(
			request, _blogsEntry.getGroupId(), _blogLayout.getPlid());

		Layout layout = (Layout)request.getAttribute(WebKeys.LAYOUT);

		Assert.assertNull(layout);
	}

	protected void addLayouts(
			boolean portletExists, boolean blogEntryWithDifferentGroup)
		throws Exception {

		_group = GroupTestUtil.addGroup();

		_blogLayout = LayoutTestUtil.addLayout(_group);
		_assetLayout = LayoutTestUtil.addLayout(_group);

		if (portletExists) {
			LayoutTestUtil.addPortletToLayout(_blogLayout, PortletKeys.BLOGS);
		}

		Map<String, String[]> preferenceMap = new HashMap<>();

		preferenceMap.put("assetLinkBehavior", new String[] {"viewInPortlet"});

		PortletInstance portletInstance = new PortletInstance(
			com.liferay.portlet.util.test.PortletKeys.TEST);

		_testPortletId = portletInstance.getPortletInstanceKey();

		LayoutTestUtil.addPortletToLayout(
			TestPropsValues.getUserId(), _assetLayout, _testPortletId,
			"column-1", preferenceMap);

		Group group = _group;

		if (blogEntryWithDifferentGroup) {
			group = GroupTestUtil.addGroup();
		}

		ServiceContext serviceContext =
			ServiceContextTestUtil.getServiceContext(
				group.getGroupId(), TestPropsValues.getUserId());

		_blogsEntry = BlogsEntryLocalServiceUtil.addEntry(
			TestPropsValues.getUserId(), RandomTestUtil.randomString(),
			RandomTestUtil.randomString(), serviceContext);
	}

	protected HttpServletRequest getHttpServletRequest() throws Exception {
		HttpServletRequest request = new MockHttpServletRequest();

		ThemeDisplay themeDisplay = getThemeDisplay();

		request.setAttribute(WebKeys.THEME_DISPLAY, themeDisplay);

		return request;
	}

	protected ThemeDisplay getThemeDisplay() throws Exception {
		ThemeDisplay themeDisplay = new ThemeDisplay();

		themeDisplay.setScopeGroupId(_group.getGroupId());

		PermissionChecker permissionChecker =
			PermissionCheckerFactoryUtil.create(TestPropsValues.getUser());

		themeDisplay.setPermissionChecker(permissionChecker);

		return themeDisplay;
	}

	private static final String[] _PORTLET_IDS = {
		PortletKeys.BLOGS_ADMIN, PortletKeys.BLOGS, PortletKeys.BLOGS_AGGREGATOR
	};

	private Layout _assetLayout;
	private Layout _blogLayout;
	private BlogsEntry _blogsEntry;

	@DeleteAfterTestRun
	private Group _group;

	private String _testPortletId;

}